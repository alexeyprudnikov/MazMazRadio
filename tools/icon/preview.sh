#!/bin/bash
# Рендер превью иконки из icon.svg (нужен macOS qlmanage). Результат — рядом с этим файлом.
cd "$(dirname "$0")" || exit 1
for mask in circle squircle; do
  sed -e 's|<use href="#art"/>|<g clip-path="url(#'$mask')"><use href="#art"/></g>|' \
      -e 's|width="108" height="108"|width="512" height="512"|' icon.svg > ".preview_$mask.svg"
  qlmanage -t -s 512 -o . ".preview_$mask.svg" >/dev/null 2>&1
  mv -f ".preview_$mask.svg.png" "icon_$mask.png"
done
sips -z 48 48 icon_circle.png --out icon_48.png >/dev/null 2>&1
rm -f .preview_*.svg
ls -1 icon_circle.png icon_squircle.png icon_48.png
