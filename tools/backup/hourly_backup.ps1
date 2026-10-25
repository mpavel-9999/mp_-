<#
Ежечасный срез проекта github_repo. Запускает Планировщик заданий Windows (задача "github_repo hourly backup").

Что сохраняет в <Dest>\<ГГГГММДД-ЧЧмм>\:
  repo.bundle      git bundle всех веток и среза рабочей копии - вместе с незакоммиченными и новыми файлами
                   (кроме .gitignore). Срез - коммит refs/backup/<ГГГГММДД-ЧЧмм> поверх HEAD; ветка, индекс
                   и рабочая копия не меняются. Ничего не коммитит в ветки и не отправляет в GitHub.
  mp_service.sql   pg_dump схемы mp_service (если пароль БД есть в MP_DB_PASSWORD / PGPASSWORD или pgpass.conf).
  info.txt         время, ветка, HEAD, список незакоммиченных изменений.

Новый срез не создаётся, если с прошлого не изменились ни файлы, ни БД.
Хранение: все срезы - KeepHours часов; первый срез каждого дня - KeepDays дней. Так же чистятся refs/backup/*.
Не работает, если на диске меньше 20 ГБ (правило о ресурсах из CLAUDE.md) - пишет об этом в журнал.
Восстановление - CLAUDE.md, раздел "Бэкап и продолжение после обрыва".
#>
param(
    [string]$Repo = 'C:\MP\Projects\github_repo',
    [string]$Dest = 'C:\MP\Backups\github_repo',
    [int]$KeepHours = 48,
    [int]$KeepDays = 14
)

# Continue: в PowerShell 5.1 вывод внешних программ в stderr (прогресс git) при Stop становится ошибкой;
# успех git и pg_dump проверяется по коду возврата
$ErrorActionPreference = 'Continue'
$git = 'C:\Program Files\Git\cmd\git.exe'
$pgDump = 'C:\Program Files\PostgreSQL\17\bin\pg_dump.exe'
$log = Join-Path $Dest 'backup.log'
$state = Join-Path $Dest 'last_state.txt'

function Write-Log([string]$text) {
    Add-Content -Path $log -Encoding UTF8 -Value ("{0:yyyy-MM-dd HH:mm:ss}  {1}" -f (Get-Date), $text)
}

function Assert-Ok([string]$what) {
    if ($LASTEXITCODE -ne 0) { throw "$what завершился с кодом $LASTEXITCODE" }
}

function Get-TextHash([string]$path) {
    if (-not (Test-Path $path)) { return '' }
    return (Get-FileHash -Algorithm SHA256 -Path $path).Hash
}

New-Item -ItemType Directory -Force -Path $Dest | Out-Null
$ts = Get-Date -Format 'yyyyMMdd-HHmm'

try {
    # Правило о ресурсах: на диске не меньше 20 ГБ
    $drive = (Get-Item $Dest).PSDrive
    if ($drive.Free -lt 20GB) {
        Write-Log ("ПРОПУСК: на диске {0}: свободно {1:N1} ГБ (< 20 ГБ)" -f $drive.Name, ($drive.Free / 1GB))
        exit 0
    }

    Set-Location $Repo
    $head = (& $git rev-parse HEAD).Trim(); Assert-Ok 'git rev-parse'
    $branch = (& $git rev-parse --abbrev-ref HEAD).Trim()

    # 1. Срез рабочей копии через временный индекс: рабочий индекс и ветка не трогаются
    $tmpIndex = Join-Path $env:TEMP "github_repo_backup_index_$ts"
    Copy-Item -Path (Join-Path $Repo '.git\index') -Destination $tmpIndex -Force
    $env:GIT_INDEX_FILE = $tmpIndex
    try {
        & $git add -A 2>&1 | Out-Null; Assert-Ok 'git add'
        $tree = (& $git write-tree).Trim(); Assert-Ok 'git write-tree'
    } finally {
        Remove-Item Env:GIT_INDEX_FILE -ErrorAction SilentlyContinue
        Remove-Item $tmpIndex -Force -ErrorAction SilentlyContinue
    }

    # 2. Дамп схемы БД во временный файл (только если пароль доступен без вопросов)
    $tmpDump = Join-Path $env:TEMP "github_repo_backup_db_$ts.sql"
    $pgpass = Join-Path $env:APPDATA 'postgresql\pgpass.conf'
    $dbPassword = if ($env:PGPASSWORD) { $env:PGPASSWORD } elseif ($env:MP_DB_PASSWORD) { $env:MP_DB_PASSWORD } else { $null }
    $dbNote = 'нет пароля (MP_DB_PASSWORD / PGPASSWORD / pgpass.conf) - БД не сохранена'
    if ($dbPassword -or (Test-Path $pgpass)) {
        if ($dbPassword) { $env:PGPASSWORD = $dbPassword }
        # --clean --if-exists: восстановление одной командой psql -f, схема пересоздаётся
        & $pgDump -h localhost -U postgres -d postgres -n mp_service --no-owner --clean --if-exists -w -f $tmpDump 2>&1 | Out-Null
        $dbNote = if ($LASTEXITCODE -eq 0) { 'mp_service.sql' } else { "pg_dump завершился с кодом $LASTEXITCODE - БД не сохранена" }
        if ($LASTEXITCODE -ne 0) { Remove-Item $tmpDump -Force -ErrorAction SilentlyContinue }
    }
    $dbHash = Get-TextHash $tmpDump

    # 3. Ничего не изменилось с прошлого среза - выходим
    $last = if (Test-Path $state) { Get-Content $state -Encoding UTF8 } else { @('', '') }
    if ($last[0] -eq $tree -and $last[1] -eq $dbHash) {
        Remove-Item $tmpDump -Force -ErrorAction SilentlyContinue
        Write-Log "без изменений (tree $($tree.Substring(0, 10)))"
    } else {
        $dir = Join-Path $Dest $ts
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
        $commit = (& $git commit-tree $tree -p $head -m "backup $ts ($branch)").Trim(); Assert-Ok 'git commit-tree'
        & $git update-ref "refs/backup/$ts" $commit; Assert-Ok 'git update-ref'
        # ветки, теги, ветки GitHub и сам срез; служебные ссылки (refs/agents/* и т.п.) не нужны
        & $git bundle create (Join-Path $dir 'repo.bundle') --branches --tags --remotes "refs/backup/$ts" 2>&1 | Out-Null
        Assert-Ok 'git bundle'
        if (Test-Path $tmpDump) { Move-Item $tmpDump (Join-Path $dir 'mp_service.sql') -Force }
        $changes = (& $git status --short) -join "`r`n"
        Set-Content -Path (Join-Path $dir 'info.txt') -Encoding UTF8 -Value @(
            "Срез:   $ts", "Ветка:  $branch", "HEAD:   $head", "Срез:   refs/backup/$ts = $commit", "БД:     $dbNote",
            '', 'Незакоммиченные изменения на момент среза:', $(if ($changes) { $changes } else { '(нет)' })
        )
        Set-Content -Path $state -Encoding UTF8 -Value @($tree, $dbHash)
        $size = (Get-ChildItem $dir | Measure-Object Length -Sum).Sum / 1MB
        Write-Log ("срез {0}: ветка {1}, HEAD {2}, {3:N1} МБ; БД: {4}" -f $ts, $branch, $head.Substring(0, 7), $size, $dbNote)
    }

    # 4. Хранение: всё - KeepHours часов, первый срез дня - KeepDays дней (папки и refs/backup/*)
    $now = Get-Date
    $all = Get-ChildItem $Dest -Directory | Where-Object { $_.Name -match '^\d{8}-\d{4}$' } | Sort-Object Name
    $firstOfDay = $all | Group-Object { $_.Name.Substring(0, 8) } | ForEach-Object { $_.Group[0].Name }
    foreach ($d in $all) {
        $t = [datetime]::ParseExact($d.Name, 'yyyyMMdd-HHmm', $null)
        $keep = ($now - $t).TotalHours -le $KeepHours -or (($firstOfDay -contains $d.Name) -and ($now - $t).TotalDays -le $KeepDays)
        if (-not $keep) { Remove-Item $d.FullName -Recurse -Force; Write-Log "удалён старый срез $($d.Name)" }
    }
    $kept = (Get-ChildItem $Dest -Directory | Where-Object { $_.Name -match '^\d{8}-\d{4}$' }).Name
    foreach ($ref in (& $git for-each-ref --format='%(refname)' refs/backup/)) {
        if ($kept -notcontains ($ref -replace '^refs/backup/', '')) { & $git update-ref -d $ref }
    }
} catch {
    Write-Log "ОШИБКА: $($_.Exception.Message)"
    exit 1
}
