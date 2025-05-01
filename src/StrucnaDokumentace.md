# DrawTwo

Aplikace DrawTwo je řešení projektu na předmět **Algoritmizace a Modelování** ve 3. ročníku
na škole Delta. Jedná se o "laciný ripoff" microsoft paint v javě, který umožňuje kreslit
základní tvary a obrázky. Jedná se o můj první projekt v jazyce Java.

## Jak aplikaci použít?
 - Naklonujte si GitHub repozitář do vašeho Java editoru
 - Spusťte aplikaci před soubor DrawingApp
 - Vesele malujte podle možností UI - Barva, tloušťka, styl a nástroj
 - Pro vyčíštění plátna stiskněte klávesu 'C'
    > Aplikace nemá oficiální výstup, vaše malůvky nelze uložit do PC
 
## Řešení
Jedná se o práci na pixelové úrovni, cílem je použít co nejvíce algoritmů a naučit
se s nimi pracovat. Používá se jen knihoven **AWT a Swing** v nativní Javě, které umožňují
práci s plátnem obecně

### Editace
editace objektů v jistých případech nefunguje úplně jako v malování. Způsobuje to
logika aplikace, která na to není tak uplně dělaná. Nakreslené tvary
nejsou brány jako objekty, ale jako prosté pixely na plátně. To je asi primární rozdíl oproti klasickému malování.