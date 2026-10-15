# «Ничего не падает» — работа с расписанием

Вы разрабатываете консольное приложение на Kotlin. Главная задача — модель занятий, которую нельзя испортить через публичный API, делегирование реестра и ошибки разбора как данные.

**Одна итоговая сдача, два логических этапа:** сначала модель и реестр, затем разбор строк и обработка команд. Срок указан в Academy.

- [Условие задания](docs/assignment.md): требования, формат файла и вывода.
- [Памятка](docs/student-guide.md): готовый запуск, чтение строк, защита коллекций и тесты.

## Что уже готово и что нужно написать

| Файл | Назначение |
|---|---|
| `app/src/main/kotlin/schedule/Cli.kt` | Готовый разбор аргументов и проверка параметров |
| `app/src/main/kotlin/schedule/Main.kt` | Готовый выбор обработчика, сообщения об ошибках аргументов и чтения |
| `app/src/main/kotlin/schedule/FileLines.kt` | Готовое чтение файла по строкам с номером строки |
| `app/src/main/kotlin/schedule/Commands.kt` | Заглушки `runPeriod`, `runType` и `runErrors`, которые вы реализуете |
| `app/src/test/kotlin/schedule/CliTest.kt` | Тесты готового запуска и чтения |

Создайте свои классы занятий, реестр, обёртку с журналом, парсер строк, типы отказов, форматтер и тесты. Готовый `SessionType` описывает метки команд; он не заменяет доменную модель. Переписывать разбор аргументов не требуется.

В исходном шаблоне корректные команды печатают `Not implemented: period`, `Not implemented: type` или `Not implemented: errors` и завершаются с кодом `2`. Замените эти заглушки своей реализацией. После завершения работы такие сообщения выводиться не должны.

## Начало работы

1. Сделайте Fork и клонируйте свою копию репозитория.
2. Установите **JDK 25**. Укажите его в `JAVA_HOME` и выберите JDK 25 для Gradle в IntelliJ IDEA.
3. Для генератора и проверки примеров нужен **Python 3.10+**.
4. Прочитайте условие и посмотрите `samples/`.
5. Размещайте свой код в `app/src/main/kotlin`, тесты — в `app/src/test/kotlin`.

В проекте используются Kotlin **2.4.10**, Gradle **9.5.1** и JUnit **6.1.3**. Kotlin и Gradle отдельно устанавливать не нужно: используйте Gradle Wrapper. Первая сборка загружает зависимости из интернета.

Команды, типы в файлах, сообщения и примеры вывода — на английском. Учебные темы, места и имена записаны латиницей. Отсутствующее значение обозначается `-`.

## Сборка и запуск

Откройте Terminal в IntelliJ IDEA или отдельный терминал **в корне проекта**, где находятся `gradlew.bat`, `settings.gradle.kts` и `samples/`. На Windows вместо `python` можно использовать `py -3`; на Linux/macOS — `python3`.

Windows PowerShell:

```powershell
.\gradlew.bat build :app:installDist
.\app\build\install\schedule\bin\schedule.bat period --file samples/clean.txt --from 2026-09-28 --to 2027-05-13
.\app\build\install\schedule\bin\schedule.bat type --file samples/dirty.txt --type EXAM
.\app\build\install\schedule\bin\schedule.bat errors --file samples/dirty.txt
```

Linux/macOS:

```bash
bash ./gradlew build :app:installDist
./app/build/install/schedule/bin/schedule period --file samples/clean.txt --from 2026-09-28 --to 2027-05-13
./app/build/install/schedule/bin/schedule type --file samples/dirty.txt --type EXAM
./app/build/install/schedule/bin/schedule errors --file samples/dirty.txt
```

`build` компилирует приложение и выполняет тесты. `installDist` создаёт скрипты запуска. Условное имя `schedule` в задании обозначает это приложение: терминал сам по себе такой команды не знает.

Во время разработки можно запускать через Gradle:

```powershell
.\gradlew.bat :app:run --args="period --file samples/clean.txt --from 2026-09-28 --to 2027-05-13"
```

Аргументы получает готовая `main(args)`; она вызывает вашу функцию обработки. Относительный путь к файлу отсчитывается от рабочей папки. Путь с пробелами заключайте в кавычки при запуске скрипта приложения.

Для проверки кодов завершения используйте скрипт приложения или `tools/verify.py`: при коде `1` или `2` команда Gradle `run` дополнительно сообщает о неуспешном завершении процесса.

## Проверка решения

После реализации и сборки:

```text
python tools/verify.py
```

Проверяются небольшие файлы, вывод всех четырёх типов, октябрьский диапазон, ошибки аргументов и чтения. Сравниваются текст и коды завершения; LF и CRLF в выводе считаются эквивалентными. Отчёт сохраняется в `verification/acceptance.json`.

| Входной файл | Содержание |
|---|---|
| `samples/clean.txt` | 200 корректных строк |
| `samples/dirty.txt` | 500 строк, 164 отказа |
| `samples/empty.txt` | Ни одной строки |

В `samples/*.expected` находятся эталоны. `clean.expected` — полный диапазон для `period`. Эталоны `*.type.expected` относятся к `--type EXAM`. `dirty.october.expected` — `period` с `--from 2026-10-01 --to 2026-10-31`; код завершения `1`, поскольку проверяется весь файл.

Добавьте собственные тесты на инварианты модели, защиту списков, делегирование, приоритет ошибок и границы фильтров: совпадение с эталонами не заменяет эти проверки.

### Дополнительный большой файл

`huge.txt` содержит два миллиона строк. Это дополнительная проверка после знакомства с коллекциями и последовательностями:

```text
python tools/generate_samples.py --huge
python tools/verify.py --huge
```

Большой файл исключён из Git. Обычная проверка его не требует. Повторно создать небольшие примеры можно командой `python tools/generate_samples.py`; не меняйте эталоны под своё решение.

### GitHub Actions

На push и pull request запускаются сборка, тесты и проверка согласованности примеров на Windows и Linux. Базовая проверка проходит уже в шаблоне: она проверяет готовый код и написанные вами тесты, **но не подтверждает выполнение всей лабораторной**.

Для полного сравнения вывода откройте Actions → schedule-lab → Run workflow и включите `acceptance`. Для большого файла дополнительно включите `huge`. Полная проверка должна пройти на готовом решении; исходные заглушки её не проходят.

**Автоматические проверки предназначены для самопроверки.** В репозитории уже есть тесты готового запуска и сравнение вывода с эталонами на заданных примерах. Их успешное прохождение помогает понять, что основные проверяемые сценарии, скорее всего, реализованы верно. Но это ограниченный набор тестов: он не доказывает правильность решения во всех случаях и не означает автоматическую сдачу работы.

**Основную проверку и итоговую оценку проводит лабник.** Он изучает код, проверяет устройство модели, соблюдение инвариантов, защиту коллекций, делегирование, обработку ошибок и пограничные случаи. Собственные тесты и объяснение принятых решений остаются обязательной частью работы.

## Сдача и описание реализации

### 1. Обоснование выбора типов (`class` vs `data class`)

Для классов занятий (`Lecture`, `Practice`, `Exam`, `Cancelled`) был выбран обычный **`class`**, а не `data class`.

**Причина выбора:**
`data class` в Kotlin автоматически генерирует метод `.copy()`, принимающий аргументы по умолчанию и вызывающий первичный конструктор. Это создаёт уязвимость для обхода инвариантов:
* Вызов `.copy(teachers = mutableList)` может передать внешнюю изменяемую коллекцию напрямую, если конструктор не защищён специальным образом.
* Класс `Cancelled` по требованию ТЗ не имеет свойств `location` и `teachers`.
* Обычный `class` полностью контролирует свой публичный API, не предоставляет неконтролируемых методов мутации/копирования и гарантирует, что объект невозможно получить в обход проверок.

---

### 2. Защита коллекций

В Kotlin тип `List<T>` является read-only интерфейсом, но **не гарантирует неизменяемости** (immutable) на платформе JVM: под ним может лежать ссылка на `java.util.ArrayList`.

Для защиты состояния модели применено защитное копирование через **`java.util.List.copyOf`**:
1. **Защита от изменения внешнего списка**: в конструкторах `Lecture`, `Practice`, `Exam` список преподавателей копируется:
   ```kotlin
   val teachers: List<String> = java.util.List.copyOf(teachers_raw)
   ```
   Если внешний код вызовет `.clear()` или `.add()` у исходного `MutableList`, объект занятия не изменится.
2. **Защита выданного списка**: метод `java.util.List.copyOf` возвращает неизменяемую реализацию списка (`ImmutableCollections`). Любая попытка вызова мутирующих методов (даже через приведение типа `(teachers as MutableList).add(...)`) приводит к `UnsupportedOperationException`.
3. **Снимки репозитория**: метод `all()` репозитория также возвращает `java.util.List.copyOf(sessions)`. Добавление новых элементов в репозиторий после вызова `all()` не меняет ранее полученный снимок.

---

### 3. Делегирование реестра (`AuditedSessionRepository`)

Репозиторий аудита реализован с использованием ключевого слова `by`:
```kotlin
class AuditedSessionRepository(private val delegate: delegate) : SessionRepository by delegate
```

#### Ответ на вопрос: Почему переопределить только `add` недостаточно для учёта вызовов `addAll`?

Делегирование интерфейса (`by delegate`) в Kotlin построено на **композиции**, а не на полиморфизме виртуальных вызовов:
1. Вызов метода `addAll(sessions)` у `AuditedSessionRepository` без явного переопределения уходит напрямую в `delegate.addAll(sessions)`.
2. Даже если реализация `delegate` внутри своего класса вызывает `this.add()`, ссылка `this` указывает на сам внутренний объект `delegate`, а **не** на внешний `AuditedSessionRepository`.
3. В результате переопределённый метод `add` обёртки **вообще не вызовется**, и аудит пропустит добавленные занятия.

Поэтому метод `addAll` обязан быть переопределён в `AuditedSessionRepository` явно.

---

### 4. Проверка «Сломай свою модель»

В тестовом классе `app/src/test/kotlin/schedule/SessionTest.kt` реализованы проверки всех 8 сценариев из задания:

| № | Попытка нарушения | Что защищает модель | Тест |
|---|---|---|---|
| 1 | Создать занятие с пустой темой | Проверка при создании: `require(topic_is_valid(topic))` | `test01_cannotCreateSessionWithEmptyOrBlankTopic` |
| 2 | Создать занятие с годом 0 или 10000 | Проверка при создании: `require(year_is_valid(date))` | `test02_cannotCreateSessionWithInvalidYear` |
| 3 | Создать действующее занятие без преподавателей | Проверка при создании: `require(teachers.isNotEmpty())` | `test03_cannotCreateActiveSessionWithoutTeachers` |
| 4 | Создать сдачу с 1 преподавателем | Проверка при создании: `require(teachers.size >= 2)` | `test04_cannotCreateExamWithLessThanTwoTeachers` |
| 5 | Создать сдачу с двумя одинаковыми преподавателями | Проверка при создании: `require(teachers_is_valid(teachers))` (проверка уникальности) | `test05_cannotCreateExamWithDuplicateTeachers` |
| 6 | Изменить исходный список после создания | Защита коллекции: `java.util.List.copyOf(teachers_raw)` отвязывает объект от внешней ссылки | `test06_mutatingOriginalListDoesNotAffectSession` |
| 7 | Изменить состояние через выданный список | Защита коллекции: `java.util.List.copyOf` возвращает JVM-неизменяемый список | `test07_cannotMutateSessionThroughReturnedTeachersList` |
| 8 | Обойти проверки через `copy` | Устройство типа: используется обычный `class`, метод `copy` отсутствует в API | `test08_copyMethodIsNotExposedOnRegularClasses` |
| - | Создать отмену со свойствами места/учителей | Устройство типа: у класса `Cancelled` свойства места и учителей отсутствуют вовсе | `test13_allFourTypesCreatedSuccessfully` |

---

### 5. Сообщения об ошибках парсера

Парсер строк (`app/src/main/kotlin/schedule/Parser.kt`) форматирует ошибки по строгому приоритету:

1. `line N: invalid field separator`
2. `line N: got X fields, expected 5`
3. `line N: empty required field "<fieldName>"`
4. `line N: invalid date "<rawValue>"`
5. `line N: unknown session type "<rawValue>"`
6. `line N: type "EXAM" requires at least 2 teachers, got X`
7. `line N: control characters in field "<fieldName>"`
8. `line N: outer whitespace in field "<fieldName>"`
9. `line N: cancelled session field "<fieldName>" must be "-"`
10. `line N: invalid active session location "<rawValue>"`
11. `line N: invalid teacher name "<rawValue>"`
12. `line N: duplicate teacher name "<name>"`

Спецсимволы экранируются: `\\`, `\"`, а управляющие и не-ASCII символы приводятся к виду `\uXXXX`.

