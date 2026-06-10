import csv
import io
import re
import sqlite3
import unicodedata
import urllib.request
import zipfile
from difflib import SequenceMatcher
from pathlib import Path


ROOT_DIR = Path(__file__).resolve().parents[1]
OUTPUT_DB = ROOT_DIR / "app" / "src" / "main" / "assets" / "database" / "cities.db"
TEMP_DIR = ROOT_DIR / "tools" / "tmp_geonames"

CITIES_ZIP_URL = "https://download.geonames.org/export/dump/cities5000.zip"
COUNTRY_INFO_URL = "https://download.geonames.org/export/dump/countryInfo.txt"
ALTERNATE_NAMES_ZIP_URL = "https://download.geonames.org/export/dump/alternateNamesV2.zip"

CITIES_ZIP_PATH = TEMP_DIR / "cities5000.zip"
ALTERNATE_NAMES_ZIP_PATH = TEMP_DIR / "alternateNamesV2.zip"


RU_COUNTRY_NAMES = {
    "RU": "Россия",
    "BY": "Беларусь",
    "UA": "Украина",
    "KZ": "Казахстан",
    "US": "США",
    "BR": "Бразилия",
    "TR": "Турция",
    "DE": "Германия",
    "FR": "Франция",
    "IT": "Италия",
    "ES": "Испания",
    "GB": "Великобритания",
    "CN": "Китай",
    "JP": "Япония",
    "KR": "Южная Корея",
    "FI": "Финляндия",
    "SE": "Швеция",
    "NO": "Норвегия",
    "PL": "Польша",
    "NL": "Нидерланды",
    "CZ": "Чехия",
    "AT": "Австрия",
    "CH": "Швейцария",
    "AE": "ОАЭ",
    "EG": "Египет",
    "TH": "Таиланд",
    "IN": "Индия",
}


MANUAL_DISPLAY_NAMES = {
    498817: "Санкт-Петербург",
    524901: "Москва",
    1486209: "Екатеринбург",
    1496747: "Новосибирск",
    1508291: "Челябинск",
    551487: "Казань",
    520555: "Нижний Новгород",
    542420: "Краснодар",
    491422: "Сочи",
    2013348: "Владивосток",
    479561: "Уфа",
    499099: "Самара",
    498677: "Саратов",
    1496153: "Омск",
    1502026: "Красноярск",
    472757: "Волгоград",
    472045: "Воронеж",
    2014407: "Улан-Удэ",
    2023469: "Иркутск",
    2123628: "Магадан",
    1493197: "Пермь",
    580922: "Архангельск",
    511196: "Пермь",
    554234: "Калининград",
    1503772: "Ханты-Мансийск",
    2013159: "Якутск",
    2026609: "Благовещенск",
    2022890: "Хабаровск",
    1490624: "Сургут",
    1488754: "Тюмень",
    472459: "Владикавказ",
    515012: "Орёл",
    548408: "Киров",
    554840: "Ижевск",
    484646: "Тамбов",
    480060: "Тверь",
    472231: "Вологда",
    2013348: "Владивосток",
}


BAD_DISPLAY_NAMES = {
    "спб",
    "питер",
    "свердловск",
    "ленинград",
    "петроград",
    "сталинград",
    "горький",
    "куйбышев",
    "калинин",
    "молотов",
    "фрунзе",
    "орджоникидзе",
    "ворошиловград",
    "целиноград",
    "андропов",
    "рыбинск-андропов",
    "кенигсберг",
    "кёнигсберг",
}


CYRILLIC_VOWELS = set("аеёиоуыэюя")


TRANSLIT_TABLE = {
    "а": "a",
    "б": "b",
    "в": "v",
    "г": "g",
    "д": "d",
    "е": "e",
    "ё": "e",
    "ж": "zh",
    "з": "z",
    "и": "i",
    "й": "y",
    "к": "k",
    "л": "l",
    "м": "m",
    "н": "n",
    "о": "o",
    "п": "p",
    "р": "r",
    "с": "s",
    "т": "t",
    "у": "u",
    "ф": "f",
    "х": "kh",
    "ц": "ts",
    "ч": "ch",
    "ш": "sh",
    "щ": "shch",
    "ъ": "",
    "ы": "y",
    "ь": "",
    "э": "e",
    "ю": "yu",
    "я": "ya",
}


def download_file(url: str, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)

    if path.exists() and path.stat().st_size > 0:
        print(f"Already downloaded: {path}")
        return

    print(f"Downloading {url}")
    with urllib.request.urlopen(url) as response:
        path.write_bytes(response.read())


def download_text(url: str) -> str:
    print(f"Downloading {url}")
    with urllib.request.urlopen(url) as response:
        return response.read().decode("utf-8")


def get_zip_member_name(zip_path: Path, expected_name: str) -> str:
    with zipfile.ZipFile(zip_path, "r") as archive:
        names = archive.namelist()

    if expected_name in names:
        return expected_name

    candidates = [name for name in names if name.endswith(expected_name)]

    if candidates:
        return candidates[0]

    raise RuntimeError(f"File {expected_name} not found in {zip_path}")


def read_text_from_zip(zip_path: Path, member_name: str) -> str:
    with zipfile.ZipFile(zip_path, "r") as archive:
        with archive.open(member_name) as file:
            return file.read().decode("utf-8")


def load_country_names() -> dict[str, str]:
    text = download_text(COUNTRY_INFO_URL)
    countries: dict[str, str] = {}

    for line in text.splitlines():
        if not line or line.startswith("#"):
            continue

        parts = line.split("\t")

        if len(parts) < 5:
            continue

        country_code = parts[0]
        english_name = parts[4]

        if country_code == "ISO":
            continue

        countries[country_code] = RU_COUNTRY_NAMES.get(country_code, english_name)

    print(f"Loaded countries: {len(countries)}")
    return countries


def normalize(value: str) -> str:
    value = value.lower()
    value = value.replace("ё", "е")
    value = value.replace("-", " ")
    value = unicodedata.normalize("NFKC", value)

    chars = []

    for char in value:
        if char.isalnum() or char.isspace():
            chars.append(char)
        else:
            chars.append(" ")

    value = "".join(chars)
    value = re.sub(r"\s+", " ", value)
    return value.strip()


def has_cyrillic(value: str) -> bool:
    return any("а" <= char.lower() <= "я" or char.lower() == "ё" for char in value)


def has_cyrillic_vowel(value: str) -> bool:
    return any(char.lower() in CYRILLIC_VOWELS for char in value)


def looks_like_abbreviation(value: str) -> bool:
    cleaned = value.replace(".", "").replace("-", "").replace(" ", "")
    letters = [char for char in cleaned if char.isalpha()]

    if len(letters) <= 3:
        return True

    uppercase_letters = [char for char in letters if char.isupper()]

    if len(letters) <= 5 and len(uppercase_letters) >= 2:
        return True

    if len(letters) <= 5 and not has_cyrillic_vowel(value):
        return True

    return False


def is_bad_display_name(value: str) -> bool:
    normalized = normalize(value)

    if not normalized:
        return True

    if normalized in BAD_DISPLAY_NAMES:
        return True

    if len(value.strip()) < 4:
        return True

    if len(value.strip()) > 80:
        return True

    if looks_like_abbreviation(value):
        return True

    if not has_cyrillic(value):
        return True

    if not has_cyrillic_vowel(value):
        return True

    if " район" in normalized:
        return True

    if " область" in normalized:
        return True

    if " республика" in normalized:
        return True

    return False


def transliterate_ru(value: str) -> str:
    result = []

    for char in value.lower():
        result.append(TRANSLIT_TABLE.get(char, char))

    return normalize("".join(result))


def similarity_to_default(candidate: str, default_name: str, ascii_name: str) -> float:
    candidate_latin = transliterate_ru(candidate)

    default_normalized = normalize(default_name)
    ascii_normalized = normalize(ascii_name)

    aliases = {
        default_normalized,
        ascii_normalized,
        default_normalized.replace("saint", "sankt"),
        default_normalized.replace("st ", "sankt "),
        ascii_normalized.replace("saint", "sankt"),
        ascii_normalized.replace("st ", "sankt "),
    }

    aliases = {item for item in aliases if item}

    if not aliases:
        return 0.0

    return max(
        SequenceMatcher(None, candidate_latin, alias).ratio()
        for alias in aliases
    )


def score_russian_name(
        candidate: str,
        default_name: str,
        ascii_name: str,
        is_preferred: bool,
        is_short: bool,
        is_colloquial: bool,
        is_historic: bool,
) -> int:
    if is_bad_display_name(candidate):
        return -10000

    if is_short or is_colloquial or is_historic:
        return -10000

    normalized = normalize(candidate)

    if normalized in BAD_DISPLAY_NAMES:
        return -10000

    score = 0

    if is_preferred:
        score += 100

    similarity = similarity_to_default(candidate, default_name, ascii_name)

    if similarity >= 0.85:
        score += 80
    elif similarity >= 0.65:
        score += 40
    elif similarity >= 0.45:
        score += 15

    if "-" in candidate:
        score += 8

    if " " in candidate:
        score += 6

    if 6 <= len(candidate) <= 35:
        score += 10

    if candidate[0].isupper():
        score += 5

    if candidate.isupper():
        score -= 50

    if "(" in candidate or ")" in candidate:
        score -= 20

    if "," in candidate:
        score -= 20

    return score


def build_search_name(*values: str) -> str:
    parts = []

    for value in values:
        if not value:
            continue

        normalized = normalize(value)

        if normalized:
            parts.append(normalized)

    result = " ".join(parts)
    result = re.sub(r"\s+", " ", result)
    return result.strip()


def collect_cities_info(cities_text: str) -> dict[int, dict[str, str]]:
    cities: dict[int, dict[str, str]] = {}
    reader = csv.reader(io.StringIO(cities_text), delimiter="\t")

    for row in reader:
        if len(row) < 19:
            continue

        try:
            geoname_id = int(row[0])
        except ValueError:
            continue

        cities[geoname_id] = {
            "default_name": row[1],
            "ascii_name": row[2],
            "country_code": row[8],
        }

    print(f"Collected city info: {len(cities)}")
    return cities


def load_russian_names(cities_info: dict[int, dict[str, str]]) -> dict[int, str]:
    download_file(ALTERNATE_NAMES_ZIP_URL, ALTERNATE_NAMES_ZIP_PATH)

    member_name = get_zip_member_name(
        ALTERNATE_NAMES_ZIP_PATH,
        "alternateNamesV2.txt",
    )

    candidates_by_city: dict[int, list[dict[str, object]]] = {}

    print("Loading Russian alternate names...")

    with zipfile.ZipFile(ALTERNATE_NAMES_ZIP_PATH, "r") as archive:
        with archive.open(member_name) as raw_file:
            text_file = io.TextIOWrapper(raw_file, encoding="utf-8")
            reader = csv.reader(text_file, delimiter="\t")

            for row in reader:
                if len(row) < 4:
                    continue

                try:
                    geoname_id = int(row[1])
                except ValueError:
                    continue

                if geoname_id not in cities_info:
                    continue

                language = row[2]
                alternate_name = row[3].strip()

                if language != "ru":
                    continue

                is_preferred = len(row) > 4 and row[4] == "1"
                is_short = len(row) > 5 and row[5] == "1"
                is_colloquial = len(row) > 6 and row[6] == "1"
                is_historic = len(row) > 7 and row[7] == "1"

                city_info = cities_info[geoname_id]

                score = score_russian_name(
                    candidate=alternate_name,
                    default_name=city_info["default_name"],
                    ascii_name=city_info["ascii_name"],
                    is_preferred=is_preferred,
                    is_short=is_short,
                    is_colloquial=is_colloquial,
                    is_historic=is_historic,
                )

                if score < 0:
                    continue

                candidates_by_city.setdefault(geoname_id, []).append(
                    {
                        "name": alternate_name,
                        "score": score,
                    }
                )

    result: dict[int, str] = {}

    for geoname_id, candidates in candidates_by_city.items():
        best_candidate = max(
            candidates,
            key=lambda item: (int(item["score"]), len(str(item["name"]))),
        )

        result[geoname_id] = str(best_candidate["name"])

    for geoname_id, manual_name in MANUAL_DISPLAY_NAMES.items():
        if geoname_id in cities_info:
            result[geoname_id] = manual_name

    print(f"Loaded Russian city names: {len(result)}")
    return result


def create_database(
        countries: dict[str, str],
        cities_text: str,
        russian_names: dict[int, str],
) -> None:
    OUTPUT_DB.parent.mkdir(parents=True, exist_ok=True)

    if OUTPUT_DB.exists():
        OUTPUT_DB.unlink()

    connection = sqlite3.connect(OUTPUT_DB)
    cursor = connection.cursor()

    cursor.execute(
        """
        CREATE TABLE cities (
            geonameId INTEGER NOT NULL PRIMARY KEY,
            name TEXT NOT NULL,
            countryName TEXT NOT NULL,
            countryCode TEXT NOT NULL,
            adminName TEXT,
            latitude REAL NOT NULL,
            longitude REAL NOT NULL,
            population INTEGER NOT NULL,
            searchName TEXT NOT NULL
        )
        """
    )

    cursor.execute("CREATE INDEX index_cities_searchName ON cities(searchName)")
    cursor.execute("CREATE INDEX index_cities_countryCode ON cities(countryCode)")
    cursor.execute("CREATE INDEX index_cities_population ON cities(population)")

    reader = csv.reader(io.StringIO(cities_text), delimiter="\t")
    inserted = 0

    for row in reader:
        if len(row) < 19:
            continue

        try:
            geoname_id = int(row[0])
            default_name = row[1]
            ascii_name = row[2]
            alternate_names = row[3]
            latitude = float(row[4])
            longitude = float(row[5])
            country_code = row[8]
            population = int(row[14] or 0)
        except ValueError:
            continue

        display_name = russian_names.get(geoname_id, default_name)
        country_name = countries.get(country_code, country_code)

        search_name = build_search_name(
            display_name,
            default_name,
            ascii_name,
            alternate_names,
        )

        cursor.execute(
            """
            INSERT INTO cities (
                geonameId,
                name,
                countryName,
                countryCode,
                adminName,
                latitude,
                longitude,
                population,
                searchName
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                geoname_id,
                display_name,
                country_name,
                country_code,
                None,
                latitude,
                longitude,
                population,
                search_name,
            ),
        )

        inserted += 1

    cursor.execute("PRAGMA user_version = 1")

    connection.commit()
    connection.close()

    print(f"Created database: {OUTPUT_DB}")
    print(f"Inserted cities: {inserted}")


def main() -> None:
    TEMP_DIR.mkdir(parents=True, exist_ok=True)

    countries = load_country_names()

    download_file(CITIES_ZIP_URL, CITIES_ZIP_PATH)
    cities_member_name = get_zip_member_name(CITIES_ZIP_PATH, "cities5000.txt")
    cities_text = read_text_from_zip(CITIES_ZIP_PATH, cities_member_name)

    cities_info = collect_cities_info(cities_text)
    russian_names = load_russian_names(cities_info)

    create_database(
        countries=countries,
        cities_text=cities_text,
        russian_names=russian_names,
    )


if __name__ == "__main__":
    main()