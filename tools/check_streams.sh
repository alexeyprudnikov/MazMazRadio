#!/usr/bin/env bash
#
# Проверка живости потоков из плейлиста.
#
#   tools/check_streams.sh [путь-к-m3u]     # по умолчанию — asset-плейлист приложения
#   ./gradlew checkPlaylist                 # то же самое через Gradle
#
# Переменные окружения:
#   TIMEOUT   секунд на один поток (по умолчанию 8)
#   PARALLEL  сколько потоков проверять одновременно (по умолчанию 8)
#
# Код возврата: 0 — все потоки живые, 1 — есть мёртвые, 2 — ошибка вызова.
#
# Live-поток не заканчивается: curl обрывается по таймауту (exit 28) — это живой
# поток, а не ошибка. Мёртвым считается тот, что ответил не-2xx или не ответил
# вовсе. После проверки обнови дату в комментарии к плейлисту.

set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TIMEOUT="${TIMEOUT:-8}"
PARALLEL="${PARALLEL:-8}"

check_one() {
    local arg="$1"
    local url="${arg%%$'\t'*}"
    local title="${arg#*$'\t'}"
    local res code http ctype size rest

    res="$(curl -sS -m "$TIMEOUT" -o /dev/null \
        -w '%{http_code} %{content_type} %{size_download}' "$url" 2>/dev/null)"
    code=$?

    http="${res%% *}"
    rest="${res#* }"
    ctype="${rest%% *}"
    size="${rest##* }"

    if [[ "$http" == 2* ]] &&
        { [[ $code -eq 0 ]] || [[ $code -eq 28 ]]; } &&
        { [[ ${size:-0} -gt 0 ]] || [[ "$ctype" == *mpegurl* || "$ctype" == *m3u8* ]]; }; then
        printf 'ok\t%s\t%s\t%s %s\n' "$url" "$title" "$http" "$ctype"
    else
        printf 'dead\t%s\t%s\t%s %s (curl %s)\n' "$url" "$title" "${http:-000}" "${ctype:-?}" "$code"
    fi
}

# Рабочая лошадка для xargs: один поток на вызов скрипта.
if [[ "${1:-}" == "--check" ]]; then
    check_one "$2"
    exit 0
fi

PLAYLIST="${1:-$SCRIPT_DIR/../app/src/main/assets/playlists/hostingradio.m3u}"
if [[ ! -f "$PLAYLIST" ]]; then
    echo "Плейлист не найден: $PLAYLIST" >&2
    exit 2
fi

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

# парами: URL <TAB> название
awk '
    /^#EXTINF:/ {
        s = $0; pos = 0
        for (i = 1; i < length(s); i++) if (substr(s, i, 2) == "\",") pos = i
        if (pos > 0) title = substr(s, pos + 2)
        else { title = s; sub(/^#EXTINF:[^,]*,/, "", title) }
        next
    }
    /^#/ || /^[[:space:]]*$/ { next }
    { printf "%s\t%s\n", $0, title }
' "$PLAYLIST" > "$work/pairs"

total="$(grep -c . "$work/pairs")"
echo "Проверяю $total потоков из $PLAYLIST (timeout ${TIMEOUT}s, параллельно $PARALLEL)"
echo

tr '\n' '\0' < "$work/pairs" |
    xargs -0 -P "$PARALLEL" -I{} bash "$BASH_SOURCE" --check {} |
    sort -t"$(printf '\t')" -k3 > "$work/results"

dead=0
# Выравнивание по символам, а не байтам: названия кириллицей,
# и printf %-42s считал бы ширину неверно.
pad() {
    local width=44
    local len=${#1}
    local spaces=$((width - len))
    ((spaces < 1)) && spaces=1
    printf '%s%*s' "$1" "$spaces" ''
}

while IFS=$'\t' read -r status url title detail; do
    if [[ "$status" == "ok" ]]; then
        printf '  ok    %s %s\n' "$(pad "$title")" "$detail"
    else
        dead=$((dead + 1))
        printf '  DEAD  %s %s\n' "$(pad "$title")" "$detail"
    fi
done < "$work/results"

ok=$((total - dead))
echo
echo "живых: $ok, мёртвых: $dead"

if [[ $dead -gt 0 ]]; then
    echo
    echo "Мёртвые: убери строку #EXTINF + URL из плейлиста и добавь их в docs/dead-streams.md"
    echo "с причиной; дату проверки в комментарии к плейлисту обнови на сегодняшнюю."
    exit 1
fi

exit 0
