package schedule

import java.time.LocalDate

fun year_is_valid(year: LocalDate): Boolean {
    return year.year <= 9999 && year.year >= 1
}

fun topic_is_valid(topic: String): Boolean {
    return topic.isNotBlank() && topic.trim() == topic && topic.none { it.isISOControl() }
}

fun location_is_valid(location: String): Boolean {
    return location.isNotBlank() && location != "-" && location.trim() == location && location.none { it.isISOControl() }
}

fun teachers_is_valid(teachers: List<String>): Boolean {
    val map = HashMap<String, Int>()
    for (teacher in teachers) {
        if (teacher.isBlank() || teacher == "-" || teacher.trim() != teacher || teacher.any { it.isISOControl() }) return false
        if (map.get(teacher) != null) return false
        map.put(teacher, 1)
    }
    return true
}

interface Session {
    val date: LocalDate
    val topic: String
}

class Lecture(
    override val date: LocalDate,
    override val topic: String,
    val location: String,
    teachers_raw: List<String>,
) : Session {
    val teachers: List<String> = java.util.List.copyOf(teachers_raw)

    init {
        require(year_is_valid(date)) { "year is invalid: $date" }
        require(topic_is_valid(topic)) { "topic is invalid: $topic" }
        require(location_is_valid(location)) { "location is invalid: $location" }
        require(teachers.isNotEmpty()) { "teachers is invalid: $teachers" }
        require(teachers_is_valid(teachers)) { "teachers is invalid: $teachers" }
    }
}

class Practice(
    override val date: LocalDate,
    override val topic: String,
    val location: String,
    teachers_raw: List<String>,
) : Session {
    val teachers: List<String> = java.util.List.copyOf(teachers_raw)

    init {
        require(year_is_valid(date)) { "year is invalid: $date" }
        require(topic_is_valid(topic)) { "topic is invalid: $topic" }
        require(location_is_valid(location)) { "location is invalid: $location" }
        require(teachers.isNotEmpty()) { "teachers is invalid: $teachers" }
        require(teachers_is_valid(teachers)) { "teachers is invalid: $teachers" }
    }
}

class Exam(
    override val date: LocalDate,
    override val topic: String,
    val location: String,
    teachers_raw: List<String>,
) : Session {
    val teachers: List<String> = java.util.List.copyOf(teachers_raw)

    init {
        require(year_is_valid(date)) { "year is invalid: $date" }
        require(topic_is_valid(topic)) { "topic is invalid: $topic" }
        require(location_is_valid(location)) { "location is invalid: $location" }
        require(teachers.size >= 2) { "teachers is invalid: $teachers" }
        require(teachers_is_valid(teachers)) { "teachers is invalid: $teachers" }
    }
}

class Cancelled(
    override val date: LocalDate,
    override val topic: String,
) : Session {
    init {
        require(year_is_valid(date)) { "year is invalid: $date" }
        require(topic_is_valid(topic)) { "topic is invalid: $topic" }
    }
}

interface SessionRepository {
    fun add(session: Session)
    fun addAll(sessions: List<Session>)
    fun all(): List<Session>
}

class delegate : SessionRepository {
    private val sessions = mutableListOf<Session>()

    override fun add(session: Session) {
        sessions.add(session)
    }

    override fun addAll(sessions: List<Session>) {
        for (session in sessions) {
            this.sessions.add(session)
        }
    }

    override fun all(): List<Session> = java.util.List.copyOf(sessions)
}

data class AuditEvent(val name: String, val count: Int)

class AuditedSessionRepository(private val delegate: delegate)
    : SessionRepository by delegate {

    private val audits = mutableListOf<AuditEvent>()

    fun audits(): List<AuditEvent> = java.util.List.copyOf(audits)

    override fun add(session: Session) {
        delegate.add(session)
        audits.add(AuditEvent("add", 1))
    }

    override fun addAll(sessions: List<Session>) {
        delegate.addAll(sessions)
        audits.add(AuditEvent("addAll", sessions.size))
    }
}








