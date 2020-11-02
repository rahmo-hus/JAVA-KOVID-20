# JAVA-KOVID-20
 
 Projektni zadatak (Elektrotehnički fakultet Banja Luka, predmet Programski jezici II 2020)
 
 Jednostavna Desktop aplikacija koja simulira kretanje stanovnika po matrici grada u kojoj djeluje virus JAVA-KOV-20. Stanovnici (stariji, odrasli i djeca) simultano se kreću po matrici grada uz odgovarajuće granice, gdje rastojanje između stanovnika iz različitih kuća mora biti najmanje dva polja. Ukoliko neki od stanovnika naruši zakon o rastojanju, on odmah mijenja smjer kretanja. Također, u gradu postoje i kontrolni punkzovi koji očitavaju temperature stanovnika koji se nađu neposredno u njegovoj blizini, te ukoliko temperatura stanovnika bude veća od 37C, po stanovnika dolazi ambulantno vozilo koje ga odvozi u ambulantu, dok supervizor šalje signal svim stanovnicima da se najkraćim rastojanjem vrate svojoj kući. 
 
 Na početku same simulacije, korisnik unosi broj stanovnika svake vrste, broj kuća u kojima će oni biti raspoređeni u zavisnosti od broja (uniformno se raspoređuju po kućama, uz poštovanje zakona da djeca ne smiju biti sama u kući), broj ambulatnih vozila, te broj kontrolnih punktova.
 Po pokretanju simulacije ispod matrice grada su ispisane informacije o svim stanovnicima (njihov identifikator, ime, trenutna temperatura (mijenja se svakih 30 s), smjer kretanja), te korisnik može u bilo kojem trenutku da pauuzira simulaciju, izveze statističke podatke u CSV fajl, te da kreira novu ambulantu, ukoliko je to potrebno. Po završetku simuacije, statistički podaci ispisuju se u datoteku SIM-JAVA-trenutno_vrijeme.txt
