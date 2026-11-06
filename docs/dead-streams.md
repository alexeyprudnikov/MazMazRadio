# Мёртвые потоки

Станции, которые были в исходном плейлисте и сняты: не отвечают, отдают 404 на mount
или 403 с чужим сертификатом. Проверка — 2026-11-06, сеть вне РФ.

В `app/src/main/assets/playlists/hostingradio.m3u` их нет. Поток ожил — вернуть
строкой `#EXTINF` + URL в нужную секцию плейлиста; категория новая — добавить
константу в `app/src/main/java/com/example/mazmazradio/data/Categories.kt` и в `ORDER`.

| Станция | Поток | Причина |
|---|---|---|
| Новое Радио | `https://emgspb.hostingradio.ru/newradio128.aacp` | mount 404 |
| Радио Дача | `https://radiodacha.hostingradio.ru/dacha_128.aacp` | 403 + чужой сертификат |
| Радио Карнавал | `https://carnaval.hostingradio.ru/carnaval128.aacp` | 403 + чужой сертификат |
| Такси FM | `https://taxifm.hostingradio.ru/taxifm128.aacp` | 403 + чужой сертификат |
| НАШЕ · Русский рок 90-х | `https://nashe1.hostingradio.ru/rock90-128.mp3` | mount 404 |
| НАШЕ · Паутина (инди и альтернатива) | `https://nashe1.hostingradio.ru/web-128.mp3` | mount 404 |
| НАШЕ · Панк-рок | `https://nashe1.hostingradio.ru/punk-128.mp3` | mount 404 |
| НАШЕ · Heavy Metal & Hard Rock | `https://nashe1.hostingradio.ru/heavy-128.mp3` | mount 404 |
| НАШЕ · Легенды рока | `https://nashe1.hostingradio.ru/legends-128.mp3` | mount 404 |
| НАШЕ · Русская волна | `https://nashe1.hostingradio.ru/rusvolna-128.mp3` | mount 404 |
| Дорожное · Шансон | `https://dorognoe.hostingradio.ru/chanson128.aacp` | mount 404 |
| Дорожное · Душевные песни | `https://dorognoe.hostingradio.ru/soul128.aacp` | mount 404 |
| DFM · Top 40 | `https://dfm.hostingradio.ru/dfmtop40128.mp3` | mount 404 |
| Радио JAZZ · Smooth Jazz | `https://nashe1.hostingradio.ru/smoothjazz-128.mp3` | mount 404 |
| Радио JAZZ · Classic Jazz | `https://nashe1.hostingradio.ru/classicjazz-128.mp3` | mount 404 |
| Радио JAZZ · Blues & Soul | `https://nashe1.hostingradio.ru/blues-128.mp3` | mount 404 |
| Радио Культура | `https://cultura.hostingradio.ru/cultura128.aacp` | 403 + чужой сертификат |
| Вести FM | `https://vestifm.hostingradio.ru/vestifm128.aacp` | 403 + чужой сертификат |
| Радио России | `https://radiorossii.hostingradio.ru/radiorossii128.aacp` | 403 + чужой сертификат |
| Радио Маяк | `https://mayak.hostingradio.ru/mayak128.aacp` | 403 + чужой сертификат |
| Говорит Москва | `https://govoritmoskva.hostingradio.ru/gm128.aacp` | 403 + чужой сертификат |
| Радио Шансон · Романтический шансон | `https://chanson.hostingradio.ru/romance128.mp3` | mount 404 |
| Радио Шансон · Блатная лирика | `https://chanson.hostingradio.ru/blat128.mp3` | mount 404 |
| Радио Шансон · Барды и авторская песня | `https://chanson.hostingradio.ru/bards128.mp3` | mount 404 |
| Comedy Radio | `https://comedy.hostingradio.ru/comedy128.aacp` | 403 + чужой сертификат |
| Радио Шоколад (98.4 FM) | `https://chokolate.hostingradio.ru/chokolate128.aacp` | 403 + чужой сертификат |
| Детское радио | `https://deti.hostingradio.ru/deti128.aacp` | 403 + чужой сертификат |
| Радио Зенит (Санкт-Петербург) | `https://zenit.hostingradio.ru/zenit128.aacp` | 403 + чужой сертификат |
| Питер FM (Санкт-Петербург) | `https://piterfm.hostingradio.ru/piterfm128.aacp` | 403 + чужой сертификат |
| Радио Модерн | `https://modern.hostingradio.ru/modern128.aacp` | 403 + чужой сертификат |
