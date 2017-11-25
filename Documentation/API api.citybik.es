Aplikacja Bike_s korzystać będzie z danych udostępnianych przez inną aplikację. Pobieranie danych odbywać będzie się za pomocą API, które to udostępnia aplikacja CityBikes. Posiada ona wszystkie wymagane informacje, jakie potrzebne będą dla użytkownika.

API dostarcza strukturę danych JSON, która to jest przetwarzana przez aplikację i odpowiednio wyświetlana użytkownikowi.

API udostępnia dwie podstawowe uporządkowane informacje:
sieci – jest to wykaz większości firm na terenie całego świata, które obsługują rowery miejskie (https://api.citybik.es/v2/networks)
stacje w danej sieci – jest to wykaz stacji obsługiwanych przez daną firmę (np. https://api.citybik.es/v2/networks/bike_s-srm-szczecin)


Struktura daych jaka udostępniana jest przez API:

1. Sieci
Struktura
{
  "networks": [ 
    {
      "company": [ciąg znaków], 
      "href": [ciąg znaków], 
      "id": [ciąg znaków], 
      "location": {
        "city": [ciąg znaków], 
        "country": [ciąg znaków], 
        "latitude": [liczba zmiennopozycyjna], 
        "longitude": [liczba zmiennopozycyjna]
      }, 
      "name": [ciąg znaków]
    }
}

Przykład
{
  "networks": [ 
    {
      "company": [
        "Bike U Sp. z o.o."
      ], 
      "href": "/v2/networks/bbbike", 
      "id": "bbbike", 
      "location": {
        "city": "Bielsko-Bia\u0142a", 
        "country": "PL", 
        "latitude": 49.8225, 
        "longitude": 19.044444
      }, 
      "name": "BBBike"
    }
}

Opis pól:
company - nazwa firmy obsługującej rowery
href - link do struktury z danymi, która zawiera informacje o stacjach
id - identyfikator sieci
city - miasto, w którym znajduje się sieć
country - kraj, w którym znajduje się sieć
latitude - szerokość geograficzna
longitude - długość geograficzna
name - skrócona nazwa sieci


2. Stacje

Struktura
{
  "network": {
    "company": [ciąg znaków], 
    "href": [ciąg znaków], 
    "id": [ciąg znaków], 
    "location": {
      "city": [ciąg znaków], 
      "country": [ciąg znaków], 
      "latitude": [liczba zmiennopozycyjna], 
      "longitude": [liczba zmiennopozycyjna]
    }, 
    "name": [ciąg znaków], 
    "stations": [
      {
        "empty_slots": [liczba stałopozycyjna], 
        "extra": {
          "bike_uids": [tablica liczb stało pozycyjnych], 
          "number": [liczba stałopozycyjna], 
          "slots": [liczba stałopozycyjna], 
          "uid": [liczba stałopozycyjna]
        }, 
        "free_bikes": [liczba stałopozycyjna], 
        "id": [ciąg znaków], 
        "latitude": [liczba zmiennopozycyjna], 
        "longitude": [liczba zmiennopozycyjna], 
        "name": [ciąg znaków], 
        "timestamp": [data]
      }
	}
}

Przykład{
  "network": {
    "company": [
      "Nextbike GmbH"
    ], 
    "href": "/v2/networks/bike_s-srm-szczecin", 
    "id": "bike_s-srm-szczecin", 
    "location": {
      "city": "Szczecin", 
      "country": "PL", 
      "latitude": 53.4301, 
      "longitude": 14.5498
    }, 
    "name": "Bike_S SRM", 
    "stations": [
      {
        "empty_slots": 10, 
        "extra": {
          "bike_uids": [
            "94388", 
            "94198", 
            "94153", 
            "94070", 
            "94606"
          ], 
          "number": "9308", 
          "slots": 16, 
          "uid": "1347728"
        }, 
        "free_bikes": 6, 
        "id": "eddd5e27a23961d851a47575211d7bf5", 
        "latitude": 53.43307, 
        "longitude": 14.53953, 
        "name": "Plac Szarych Szereg\u00f3w", 
        "timestamp": "2017-11-25T16:39:21.845000Z"
      }
	}
}

Opis pól
Na samym początku struktura zawiera informację o sieci, której stacje są wyświetlane z polami:
company - nazwa firmy obsługującej rowery
href - link do struktury z danymi, która zawiera informacje o stacjach
id - identyfikator sieci
city - miasto, w którym znajduje się sieć
country - kraj, w którym znajduje się sieć
latitude - szerokość geograficzna
longitude - długość geograficzna
name - skrócona nazwa sieci

Następnie zaczyna się struktura każdej ze stacji z polami:
empty_slots - liczba wolnych miejsc na rowery
bike_uids - tablica zawierająca identyfikatory rowerów znajdujących się w stacji
number - numer stacji
slots - liczba miejsc na stacji
uid - ...
free_bikes - liczba rowerów obecnych na stacji
id - indywidualny identyfikator stacji
latitude - szerokość geograficzna
longitude - długość geograficzna
name - nazwa stacji
timestamp - data i czas ostatniego odświeżenia
