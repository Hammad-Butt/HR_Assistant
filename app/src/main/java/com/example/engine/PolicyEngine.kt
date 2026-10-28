package com.example.engine

import com.example.data.DocumentItem
import com.example.data.PolicyAnswer
import java.util.Locale
import java.util.regex.Pattern

object PolicyEngine {

    val REFUSAL_MESSAGE = "This doesn't appear to be covered by any of the uploaded HR documents."

    private val STOP_WORDS = setOf(
        "the", "a", "an", "is", "are", "was", "were", "be", "been", "being", "to", "of",
        "in", "on", "at", "by", "for", "with", "about", "as", "into", "through", "during",
        "before", "after", "above", "below", "from", "up", "down", "and", "or", "but", "if",
        "then", "else", "when", "where", "what", "which", "who", "whom", "why", "how", "this",
        "that", "these", "those", "it", "its", "i", "you", "he", "she", "we", "they", "them",
        "my", "your", "his", "her", "our", "their", "am", "has", "have", "had", "do", "does",
        "did", "will", "would", "should", "could", "can", "may", "might", "must", "shall", "so",
        "than", "too", "very", "just", "not", "no", "nor", "only", "own", "same", "such", "more",
        "most", "some", "any", "all", "each", "every", "both", "few", "other", "another", "also",
        "me", "us", "him", "yes", "ok", "okay", "please", "thanks", "thank", "dear", "hello",
        "hi", "hey", "tell", "give", "know", "want", "need", "like", "get", "got"
    )

    val SUGGESTION_TOPICS = listOf(
        "annual leave", "sick leave", "maternity leave", "paternity leave", "unpaid leave",
        "working hours", "attendance", "overtime", "holiday", "public holiday", "dress code",
        "probation", "resignation", "notice period", "salary", "payroll", "benefits",
        "insurance", "medical", "remote work", "work from home", "travel", "expenses",
        "performance review", "training", "code of conduct", "disciplinary"
    )

    fun detectGreeting(rawText: String): String? {
        val t = rawText.lowercase(Locale.ROOT)
            .replace("-", " ")
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (t.isEmpty() || t.length > 120) return null

        // Catch casual and formal greetings: hello, hy, hi, hey, yo, wassup, sup, etc.
        val greetingPattern = Regex(
            "^(hi+|hy+|hey+|heya+|hiya+|hie+|hello+|helo+|hellow+|hola+|yo+|yoo+|yooo+|sup+|wassup+|wazzup+|whats\\s*up|what\\s*s\\s*up|howdy+|greetings?|salutations?|salam+|salaam+|assalam[\\sou]*alaikum|asalam[\\sou]*alaikum|salam[\\sou]*alaikum|aoa|walaikum\\s*(assalam|salam)|good\\s*(morning|afternoon|evening|day|night)|gm|gn|ge|bonjour|namaste|ciao)(\\s+(there|hr|assistant|bot|team|sir|madam|all|everyone|folks|friend|bro|buddy|pal|dude|man|dear|here|again))?$"
        )

        if (greetingPattern.matches(t)) {
            return "Hello! 👋 I'm your HR Assistant. Ask me anything about company policies — leave, attendance, working hours, dress code, benefits, and more. I answer directly from the HR documents on this device."
        }

        // Casual "how are you" checks
        if (Regex("^(how\\s*(are|r)\\s*(you|u)(\\s*(doing|today|now|these\\s*days|bro|sir|madam|buddy|assistant|hr))?|how('?s|\\s*is)\\s*(it\\s*going|everything|things|life)|how\\s*do\\s*you\\s*do|kya\\s*haal(\\s*\\w*)?|kia\\s*hal(\\s*\\w*)?|whats\\s*good|what\\s*s\\s*good)$").matches(t)) {
            return "I'm here and ready to help! 🙂 What would you like to know about company policies?"
        }

        // Thank you expressions
        if (Regex("^(thanks?|thank\\s*you|thanku|thnx|thx|ty|shukriya|shukria|jazak\\s*allah|jazakallah|many\\s*thanks|much\\s*appreciated|appreciate\\s*it)(\\s*(so\\s*much|a\\s*lot|very\\s*much|bro|sir|madam|dear|hr|assistant))?$").matches(t)) {
            return "You're welcome! 😊 Let me know if there's anything else you'd like to ask about HR policies."
        }

        // Goodbyes
        if (Regex("^(bye+|goodbye+|good\\s*bye|see\\s*(you|ya)|take\\s*care|khuda\\s*hafiz|allah\\s*hafiz|cya|catch\\s*you\\s*later)(\\s*(soon|later|bro|sir|madam|hr))?$").matches(t)) {
            return "Goodbye! 👋 Come back anytime you have a question about HR policies."
        }

        // Acknowledgements
        if (Regex("^(ok|okay|k|cool|got\\s*it|noted|alright|all\\s*right|fine|great|nice|good|perfect|sure|yep|yeah|yes|no|nope|hmm+|huh|ah|oh|i\\s*see)(\\s*(thanks?|thank\\s*you|bro|hr))?$").matches(t)) {
            return "👍 Got it. If you have a question about company policies, just ask."
        }

        // Help and identity queries
        if (Regex("^(who\\s*are\\s*you|what\\s*are\\s*you|what\\s*can\\s*you\\s*do|what\\s*do\\s*you\\s*do|how\\s*(can|do)\\s*(you|i)\\s*(help|use)|help|help\\s*me|what\\s*is\\s*this|what'?s\\s*this|what\\s*is\\s*hr\\s*assistant|what\\s*is\\s*hr\\s*assistant\\s*for)$").matches(t)) {
            return "I'm your HR Assistant. I answer questions about company policies — annual leave, sick leave, attendance, working hours, dress code, benefits and other HR rules — using only the documents HR has uploaded on this device.\n\nTry asking:\n• \"What is the annual leave policy?\"\n• \"How does sick leave work?\"\n• \"What are the working hours?\"\n\nIf the answer isn't in the documents, I'll say so rather than guess."
        }

        return null
    }

    fun stripGreeting(rawText: String): String {
        val s = rawText.replace(
            Regex("^\\s*(hi+|hy+|hey+|heya+|hiya+|hello+|helo+|yo+|sup+|wassup+|salam+|salaam+|assalam[\\sou]*alaikum|asalam[\\sou]*alaikum|aoa|good\\s*(morning|afternoon|evening|day))[\\s,!.]+", RegexOption.IGNORE_CASE),
            ""
        ).trim()
        return if (s.length >= 5 && s != rawText) s else rawText
    }

    fun tokenize(text: String): List<String> {
        val matcher = Pattern.compile("[\\p{L}\\p{N}]+").matcher(text.lowercase(Locale.ROOT))
        val tokens = mutableListOf<String>()
        while (matcher.find()) {
            tokens.add(matcher.group())
        }
        return tokens
    }

    private fun wordsMatch(a: String, b: String): Boolean {
        if (a == b) return true
        val minLen = minOf(a.length, b.length)
        if (minLen < 4) return false
        var count = 0
        while (count < a.length && count < b.length && a[count] == b[count]) {
            count++
        }
        return count >= 4 && count >= minLen * 0.6
    }

    private val SYNONYMS = mapOf(
        "vacation" to listOf("annual", "leave", "holiday"),
        "pto" to listOf("annual", "leave", "paid"),
        "timeoff" to listOf("annual", "sick", "leave"),
        "wfh" to listOf("remote", "work", "home", "hybrid"),
        "telecommute" to listOf("remote", "work"),
        "stipend" to listOf("reimbursement", "allowance", "expense"),
        "pay" to listOf("salary", "payroll", "compensation", "wage"),
        "salary" to listOf("pay", "payroll", "wages"),
        "wages" to listOf("salary", "payroll", "hourly"),
        "quit" to listOf("resignation", "notice", "resign"),
        "resigning" to listOf("resignation", "notice", "resign"),
        "leaving" to listOf("resignation", "notice", "offboarding"),
        "doctor" to listOf("sick", "medical", "certificate"),
        "illness" to listOf("sick", "medical", "absence"),
        "attire" to listOf("dress", "code", "casual"),
        "clothes" to listOf("dress", "code"),
        "schedule" to listOf("hours", "working", "attendance"),
        "late" to listOf("tardiness", "attendance", "punctuality"),
        "probationary" to listOf("probation"),
        "maternity" to listOf("parental", "leave", "mother"),
        "paternity" to listOf("parental", "leave", "father"),
        "401k" to listOf("retirement", "savings", "pension"),
        "insurance" to listOf("medical", "dental", "vision", "health", "benefits")
    )

    private fun expandQueryTerms(rawTerms: List<String>): List<String> {
        val expanded = mutableListOf<String>()
        for (t in rawTerms) {
            expanded.add(t)
            SYNONYMS[t]?.let { syns -> expanded.addAll(syns) }
        }
        return expanded.distinct()
    }

    fun hitCount(text: String, qTerms: List<String>): Int {
        val tokenSet = tokenize(text).filter { it.length > 1 }.toSet()
        val expandedTerms = expandQueryTerms(qTerms)
        var hits = 0
        for (q in expandedTerms) {
            if (tokenSet.contains(q)) {
                hits++
                continue
            }
            for (candidate in tokenSet) {
                if (wordsMatch(q, candidate)) {
                    hits++
                    break
                }
            }
        }
        return hits
    }

    private data class ScoredChunk(val text: String, val docName: String, val hits: Int, val coverage: Float)

    fun retrieve(query: String, docs: List<DocumentItem>, topK: Int = 3): Pair<List<Pair<String, String>>, List<String>> {
        val baseTerms = tokenize(query).filter { it.length > 1 && !STOP_WORDS.contains(it) }.distinct()
        if (baseTerms.isEmpty()) return emptyList<Pair<String, String>>() to baseTerms

        val expandedTerms = expandQueryTerms(baseTerms)
        val all = docs.flatMap { doc -> doc.chunks.map { chunk -> chunk to doc.name } }
        val scored = all.map { (chunkText, docName) ->
            val hits = hitCount(chunkText, baseTerms)
            val coverage = hits.toFloat() / expandedTerms.size.coerceAtLeast(1)
            ScoredChunk(chunkText, docName, hits, coverage)
        }.sortedWith(compareByDescending<ScoredChunk> { it.hits }.thenByDescending { it.coverage })

        val filtered = scored.filter { it.hits > 0 }.take(topK).map { it.text to it.docName }
        return filtered to baseTerms
    }

    private fun isHeading(line: String): Boolean {
        val t = line.trim()
        if (t.isEmpty()) return false
        if (t.matches(Regex("^\\d+(\\.\\d+)*\\.?\\s+\\S.*")) && t.length < 80) return true
        if (t.startsWith("section ", ignoreCase = true)) return true
        return t.matches(Regex("^[A-Z][A-Z0-9\\s&#'.,/\\-()]{4,70}$"))
    }

    private data class UnitItem(val text: String, val pos: Int, val score: Int)

    private fun splitUnits(text: String): List<String> {
        val units = mutableListOf<String>()
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            if (isHeading(line) || line.startsWith("---")) continue
            if (line.startsWith("•") || line.startsWith("- ") || line.startsWith("* ")) {
                val b = line.replace(Regex("^[•\\-*]\\s*"), "").trim()
                if (b.length > 8) units.add(b)
                continue
            }
            val sentences = line.split(Regex("(?<=[.!?])\\s+")).map { it.trim().replace(Regex("^[-–—]\\s*"), "") }
            for (s in sentences) {
                if (s.length > 15) units.add(s)
            }
        }
        return units.distinct()
    }

    private fun deriveTitle(qTerms: List<String>, text: String): String {
        for (line in text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.take(10)) {
            val m = Regex("^\\d+(?:\\.\\d+)*\\.?\\s+(.+)$").find(line)
            if (m != null) {
                val candidate = m.groupValues[1].trim()
                if (candidate.length in 3..60 && candidate[0].isUpperCase()) {
                    return toTitleCase(candidate)
                }
            }
        }
        val skip = setOf("policy", "policies", "rule", "rules", "regulation", "regulations", "company", "employee", "employees", "department")
        var use = qTerms.filter { !skip.contains(it) && it.length > 2 }
        if (use.isEmpty()) use = qTerms.take(3)
        return use.take(3).joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } } + " Policy"
    }

    private fun toTitleCase(input: String): String {
        val words = input.split(Regex("\\s+"))
        return words.joinToString(" ") { word ->
            if (word.length > 1 && word.all { it.isUpperCase() }) {
                word.lowercase().replaceFirstChar { it.uppercase() }
            } else {
                word.replaceFirstChar { it.uppercase() }
            }
        }
    }

    fun answerQuery(rawQuery: String, docs: List<DocumentItem>): PolicyAnswer {
        val cleanQuery = stripGreeting(rawQuery)
        val (results, qTerms) = retrieve(cleanQuery, docs)

        if (results.isEmpty()) {
            return PolicyAnswer(isRefusal = true, refusalText = REFUSAL_MESSAGE)
        }

        val topPassages = results.take(3)
        val combinedText = topPassages.joinToString("\n\n---\n\n") { it.first }
        val docName = topPassages[0].second

        val rawUnits = splitUnits(combinedText)
        val expandedQTerms = expandQueryTerms(qTerms)
        val scoredUnits = rawUnits.mapIndexed { idx, u ->
            UnitItem(text = u, pos = idx, score = hitCount(u, expandedQTerms))
        }

        var chosen = scoredUnits.filter { it.score > 0 }.sortedByDescending { it.score }
        if (chosen.isEmpty()) {
            chosen = scoredUnits.take(5)
        } else if (chosen.size < 3) {
            val chosenPositions = chosen.map { it.pos }.toSet()
            val extra = scoredUnits.filter { !chosenPositions.contains(it.pos) }.take(3 - chosen.size)
            chosen = (chosen + extra)
        }
        chosen = chosen.sortedBy { it.pos }.take(6)

        if (chosen.isEmpty()) {
            return PolicyAnswer(isRefusal = true, refusalText = REFUSAL_MESSAGE)
        }

        val title = deriveTitle(qTerms, combinedText)
        val bullets = chosen.map { it.text }

        return PolicyAnswer(
            title = title,
            bullets = bullets,
            sourceName = docName,
            sourceText = combinedText,
            qTerms = qTerms,
            isRefusal = false
        )
    }

    fun chunkText(raw: String, max: Int = 1200, overlap: Int = 200): List<String> {
        val normalized = raw.replace("\r\n", "\n")
            .replace(Regex("[\\u2022\\u25CF\\u25AA\\u2023\\u2043\\u2219\\u25E6\\u00B7\\uF0B7\\u00A7]"), "•")
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex(" *\\n *"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

        val blocks = normalized.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
        val chunks = mutableListOf<String>()
        var cur = ""

        for (b in blocks) {
            if (b.length > max * 1.5) {
                if (cur.isNotEmpty()) {
                    chunks.add(cur.trim())
                    cur = ""
                }
                var sub = ""
                for (l in b.split("\n").map { it.trim() }.filter { it.isNotEmpty() }) {
                    if (sub.length + l.length + 1 > max && sub.isNotEmpty()) {
                        chunks.add(sub.trim())
                        val overlapText = if (sub.length > overlap) sub.takeLast(overlap) else sub
                        sub = overlapText + "\n" + l
                    } else {
                        sub += (if (sub.isNotEmpty()) "\n" else "") + l
                    }
                }
                if (sub.trim().isNotEmpty()) {
                    chunks.add(sub.trim())
                }
                continue
            }

            if (cur.length + b.length + 2 > max && cur.isNotEmpty()) {
                chunks.add(cur.trim())
                val overlapText = if (cur.length > overlap) cur.takeLast(overlap) else cur
                cur = overlapText + "\n\n" + b
            } else {
                cur += (if (cur.isNotEmpty()) "\n\n" else "") + b
            }
        }
        if (cur.trim().isNotEmpty()) {
            chunks.add(cur.trim())
        }

        return chunks.filter { it.length > 30 }
    }

    fun getSuggestions(docs: List<DocumentItem>): List<String> {
        if (docs.isEmpty()) return emptyList()
        val allText = docs.flatMap { it.chunks }.joinToString(" ").lowercase(Locale.ROOT)
        val found = SUGGESTION_TOPICS.filter { allText.contains(it) }.take(3).toMutableList()
        for (topic in SUGGESTION_TOPICS) {
            if (found.size >= 3) break
            if (!found.contains(topic)) found.add(topic)
        }
        return found.map { "What is the $it policy?" }
    }

    fun getDefaultHandbookDocument(): DocumentItem {
        val sampleHandbook = """
1. Annual Leave Policy
Employees are entitled to 20 days of paid annual leave per calendar year, accrued proportionally on a monthly basis.
Annual leave must be requested through the HR portal at least two weeks in advance and approved by your department manager.
A maximum of 5 unused annual leave days may be carried over into the following calendar year, and must be used within the first quarter by March 31.
Employees leaving the organization will be paid for any earned, unused annual leave in their final paycheck.

2. Sick Leave and Medical Absence
Full-time employees receive 10 paid sick leave days per year, credited on January 1st of each calendar year.
Employees must notify their supervisor and HR by 9:00 AM on the first day of illness or unanticipated medical absence.
A certified doctor's note or medical certificate is mandatory for any sick leave exceeding three consecutive working days.
Unused sick leave does not roll over to the following year and is not eligible for cash payout upon termination of employment.

3. Working Hours, Flexibility, and Attendance
Standard office working hours are Monday through Friday from 9:00 AM to 5:00 PM, totaling 40 working hours per week.
Core collaboration hours are from 10:00 AM to 4:00 PM, with flexible arrival permitted between 8:00 AM and 10:00 AM.
Employees are entitled to a one-hour unpaid lunch break each day, along with two 15-minute paid rest intervals.
Repeated tardiness or unauthorized absences without prior notice may lead to progressive disciplinary review.

4. Overtime and Compensatory Time
Overtime applies to non-exempt employees who work in excess of 40 hours in a standard workweek.
All overtime work must be pre-approved in writing by the direct department manager before the hours are performed.
Approved overtime is compensated at 1.5 times the employee's regular hourly wage rate.
Exempt salaried employees are not eligible for overtime compensation but may request compensatory time off for extraordinary project demands.

5. Remote Work and Hybrid Work Policy
Eligible employees may work remotely up to two days per week following successful completion of the three-month probation period.
Remote work arrangements must be coordinated with your team leader to ensure consistent departmental coverage.
Employees working remotely must maintain a secure, high-speed internet connection and remain reachable during core working hours.
The company provides a standard ergonomic workstation stipend of $350 for home office setup expenses.

6. Dress Code and Workplace Etiquette
Our workplace observes a professional business casual dress code from Monday through Thursday.
Casual Fridays permit clean jeans, company t-shirts, and sneakers, provided there are no client-facing meetings scheduled.
Employees attending external client presentations, executive board meetings, or regulatory proceedings must wear formal business attire.
Clothing with offensive slogans, frayed tears, or sportswear is not permitted in the workplace.

7. Benefits, Health, and Retirement
Comprehensive medical, dental, and vision insurance coverage becomes active on the first day of the month following the hire date.
The company contributes 80% toward individual employee premium costs and 50% toward eligible dependent coverage.
A 401(k) retirement savings plan is available immediately upon hire, featuring a 100% employer match up to the first 4% of salary contributed.
Additional wellness benefits include an annual gym membership reimbursement of up to $500.

8. Resignation, Notice Period, and Offboarding
Employees choosing to resign are requested to provide a minimum of two weeks (14 days) written notice to HR and their direct supervisor.
Management and leadership roles require a minimum of 30 days notice to ensure proper transition of responsibilities.
An exit interview with HR will be scheduled during the final week of employment to review benefits continuation and return company equipment.
Company-issued laptops, access badges, credit cards, and property must be returned to HR on or before the final working day.
        """.trimIndent()

        val chunks = chunkText(sampleHandbook, max = 1000, overlap = 150)
        return DocumentItem(
            id = "doc_default_sample_handbook",
            name = "Company Policy Handbook.docx",
            chunks = chunks,
            uploadedAt = System.currentTimeMillis() - 86400000L * 3,
            size = sampleHandbook.toByteArray().size.toLong()
        )
    }
}
