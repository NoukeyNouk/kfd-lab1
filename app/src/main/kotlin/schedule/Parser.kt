package schedule

import java.time.LocalDate
import java.time.format.DateTimeParseException

sealed interface ParseFailureReason {
    data object InvalidFieldSeparator : ParseFailureReason
    data class FieldCountMismatch(val actual: Int, val expected: Int) : ParseFailureReason
    data class EmptyField(val fieldName: String) : ParseFailureReason
    data class InvalidDate(val rawValue: String) : ParseFailureReason
    data class UnknownType(val rawValue: String) : ParseFailureReason
    data class ExamRequiresTwoTeachers(val actual: Int) : ParseFailureReason
    data class ControlCharacters(val fieldName: String) : ParseFailureReason
    data class OuterWhitespace(val fieldName: String) : ParseFailureReason
    data class InvalidCancelledField(val fieldName: String) : ParseFailureReason
    data class InvalidLocation(val rawValue: String) : ParseFailureReason
    data class InvalidTeacher(val rawValue: String) : ParseFailureReason
    data class DuplicateTeacher(val name: String) : ParseFailureReason
}

sealed interface ParseResult {
    data class Success(val session: Session) : ParseResult
    data class Failure(val lineNumber: Long, val reason: ParseFailureReason) : ParseResult
}

fun escapeString(value: String): String {
    val sb = StringBuilder()
    for (ch in value) {
        when {
            ch == '\\' -> sb.append("\\\\")
            ch == '"' -> sb.append("\\\"")
            ch.code < 0x20 || ch.code in 0x7F..0x9F || ch.code > 0x7E -> {
                sb.append(String.format("\\u%04X", ch.code))
            }
            else -> sb.append(ch)
        }
    }
    return sb.toString()
}

fun formatFailure(failure: ParseResult.Failure): String =
    "line ${failure.lineNumber}: ${formatReason(failure.reason)}"

fun formatReason(reason: ParseFailureReason): String = when (reason) {
    is ParseFailureReason.InvalidFieldSeparator -> "invalid field separator"
    is ParseFailureReason.FieldCountMismatch -> "got ${reason.actual} fields, expected ${reason.expected}"
    is ParseFailureReason.EmptyField -> "empty required field \"${escapeString(reason.fieldName)}\""
    is ParseFailureReason.InvalidDate -> "invalid date \"${escapeString(reason.rawValue)}\""
    is ParseFailureReason.UnknownType -> "unknown session type \"${escapeString(reason.rawValue)}\""
    is ParseFailureReason.ExamRequiresTwoTeachers -> "type \"EXAM\" requires at least 2 teachers, got ${reason.actual}"
    is ParseFailureReason.ControlCharacters -> "control characters in field \"${escapeString(reason.fieldName)}\""
    is ParseFailureReason.OuterWhitespace -> "outer whitespace in field \"${escapeString(reason.fieldName)}\""
    is ParseFailureReason.InvalidCancelledField -> "cancelled session field \"${escapeString(reason.fieldName)}\" must be \"-\""
    is ParseFailureReason.InvalidLocation -> "invalid active session location \"${escapeString(reason.rawValue)}\""
    is ParseFailureReason.InvalidTeacher -> "invalid teacher name \"${escapeString(reason.rawValue)}\""
    is ParseFailureReason.DuplicateTeacher -> "duplicate teacher name \"${escapeString(reason.name)}\""
}

private fun hasOuterWhitespace(s: String): Boolean =
    s.isNotEmpty() && (s.first().isWhitespace() || s.last().isWhitespace())

private fun hasControlCharacters(s: String): Boolean =
    s.any { it.isISOControl() }

fun parseSessionLine(lineNumber: Long, line: String): ParseResult {
    if (!line.contains('|')) {
        return ParseResult.Failure(lineNumber, ParseFailureReason.InvalidFieldSeparator)
    }

    val parts = line.split('|')
    if (parts.size != 5) {
        return ParseResult.Failure(lineNumber, ParseFailureReason.FieldCountMismatch(parts.size, 5))
    }

    val fieldNames = listOf("date", "type", "topic", "location", "teachers")
    for (i in parts.indices) {
        if (parts[i].isBlank()) {
            return ParseResult.Failure(lineNumber, ParseFailureReason.EmptyField(fieldNames[i]))
        }
    }

    val rawDate = parts[0]
    val dateRegex = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
    if (!dateRegex.matches(rawDate)) {
        return ParseResult.Failure(lineNumber, ParseFailureReason.InvalidDate(rawDate))
    }
    val date = try {
        val parsed = LocalDate.parse(rawDate)
        if (parsed.year !in 1..9999) {
            return ParseResult.Failure(lineNumber, ParseFailureReason.InvalidDate(rawDate))
        }
        parsed
    } catch (_: DateTimeParseException) {
        return ParseResult.Failure(lineNumber, ParseFailureReason.InvalidDate(rawDate))
    }

    val rawType = parts[1]
    val validTypes = setOf("LECTURE", "PRACTICE", "EXAM", "CANCELLED")
    if (rawType !in validTypes) {
        return ParseResult.Failure(lineNumber, ParseFailureReason.UnknownType(rawType))
    }

    val rawTeachersList = parts[4].split(',')
    if (rawType == "EXAM") {
        val validTeacherCount = rawTeachersList.count { it.isNotBlank() && it != "-" }
        if (validTeacherCount < 2) {
            return ParseResult.Failure(lineNumber, ParseFailureReason.ExamRequiresTwoTeachers(validTeacherCount))
        }
    }

    for (i in parts.indices) {
        if (hasControlCharacters(parts[i])) {
            return ParseResult.Failure(lineNumber, ParseFailureReason.ControlCharacters(fieldNames[i]))
        }
    }

    for (i in parts.indices) {
        if (hasOuterWhitespace(parts[i])) {
            return ParseResult.Failure(lineNumber, ParseFailureReason.OuterWhitespace(fieldNames[i]))
        }
    }

    if (rawType == "CANCELLED") {
        if (parts[3] != "-") {
            return ParseResult.Failure(lineNumber, ParseFailureReason.InvalidCancelledField("location"))
        }
        if (parts[4] != "-") {
            return ParseResult.Failure(lineNumber, ParseFailureReason.InvalidCancelledField("teachers"))
        }
    } else {
        if (parts[3] == "-") {
            return ParseResult.Failure(lineNumber, ParseFailureReason.InvalidLocation(parts[3]))
        }
        for (teacher in rawTeachersList) {
            if (teacher.isBlank() || teacher == "-" || hasOuterWhitespace(teacher) || hasControlCharacters(teacher)) {
                return ParseResult.Failure(lineNumber, ParseFailureReason.InvalidTeacher(teacher))
            }
        }
        val seenTeachers = mutableSetOf<String>()
        for (teacher in rawTeachersList) {
            if (!seenTeachers.add(teacher)) {
                return ParseResult.Failure(lineNumber, ParseFailureReason.DuplicateTeacher(teacher))
            }
        }
    }

    val topic = parts[2]
    val location = parts[3]
    val session = when (rawType) {
        "LECTURE" -> Lecture(date, topic, location, rawTeachersList)
        "PRACTICE" -> Practice(date, topic, location, rawTeachersList)
        "EXAM" -> Exam(date, topic, location, rawTeachersList)
        "CANCELLED" -> Cancelled(date, topic)
        else -> error("Unknown type: $rawType")
    }
    return ParseResult.Success(session)
}
