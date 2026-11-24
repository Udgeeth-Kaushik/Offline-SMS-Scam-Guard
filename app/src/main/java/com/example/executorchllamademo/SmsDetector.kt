package com.example.executorchllamademo

import android.content.Context
import org.json.JSONObject
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/** Signals the rule layer can raise. `reason` is the English phrase fed to Llama. */
enum class Signal(val reason: String, val hinglish: String) {
    LINK("contains a suspicious or shortened link", "isme suspicious link hai"),
    URGENCY("asks for urgent action", "jaldi karne ka dabav bana raha hai"),
    KYC("asks for KYC update", "KYC update maang raha hai"),
    OTP_REQUEST("asks for OTP or PIN", "OTP/PIN maang raha hai"),
    PRIZE("promises a prize or easy money", "inaam ya aasaan paise ka lalach de raha hai"),
    JOB("offers a suspicious job", "shaq wali job ka offer de raha hai"),
    MONEY("asks for a fee or payment", "paise ya fees maang raha hai"),
    CALL("asks to call or message a number", "kisi number par call/message karne ko keh raha hai")
}

enum class RiskLevel { LOW, MEDIUM, HIGH }

data class SmsResult(
    val modelScore: Double,   // raw TF-IDF + Logistic Regression probability of "suspicious"
    val finalScore: Double,   // after rule layer
    val signals: List<Signal>,
    val level: RiskLevel
) {
    val isSuspicious: Boolean get() = finalScore >= 0.5
}

/**
 * On-device SMS scam detector.
 * Stage 1: word 1-2 gram TF-IDF + Logistic Regression (weights in assets/sms_model.json).
 * Stage 2: small rule layer (link + urgency + KYC/OTP...) that adjusts the score.
 * No network, no cloud.
 */
class SmsDetector private constructor(
    private val vocab: HashMap<String, Int>,
    private val idf: DoubleArray,
    private val coef: DoubleArray,
    private val intercept: Double
) {

    // ---- Stage 1: model (must match sklearn; verified diff ~1e-16 in Python) ----
    fun modelScore(text: String): Double {
        val toks = ArrayList<String>()
        val m = TOKEN.matcher(text.lowercase(Locale.ROOT))
        while (m.find()) toks.add(m.group())
        val tf = HashMap<String, Int>()
        for (t in toks) if (vocab.containsKey(t)) tf[t] = (tf[t] ?: 0) + 1
        for (i in 0 until toks.size - 1) {
            val g = toks[i] + " " + toks[i + 1]
            if (vocab.containsKey(g)) tf[g] = (tf[g] ?: 0) + 1
        }
        if (tf.isEmpty()) return sigmoid(intercept)
        var norm = 0.0
        val w = HashMap<String, Double>()
        for ((g, c) in tf) {
            val v = (1.0 + ln(c.toDouble())) * idf[vocab[g]!!]
            w[g] = v
            norm += v * v
        }
        norm = sqrt(norm)
        if (norm == 0.0) norm = 1.0
        var z = intercept
        for ((g, v) in w) z += coef[vocab[g]!!] * v / norm
        return sigmoid(z)
    }

    // ---- Stage 2: rule layer ----
    fun analyze(text: String): SmsResult {
        val p = modelScore(text)
        val t = text.lowercase(Locale.ROOT)
        val signals = ArrayList<Signal>()

        val link = URL.containsMatchIn(t) || SHORT.containsMatchIn(t)
        if (link) signals.add(Signal.LINK)
        var core = 0
        if (URG.containsMatchIn(t)) { signals.add(Signal.URGENCY); core++ }
        if (KYC.containsMatchIn(t)) { signals.add(Signal.KYC); core++ }
        if (OTPR.containsMatchIn(t) && !OTPW.containsMatchIn(t)) { signals.add(Signal.OTP_REQUEST); core++ }
        if (PRIZE.containsMatchIn(t)) { signals.add(Signal.PRIZE); core++ }
        if (JOB.containsMatchIn(t)) { signals.add(Signal.JOB); core++ }
        if (MONEY.containsMatchIn(t)) { signals.add(Signal.MONEY); core++ }
        if (CALL.containsMatchIn(t)) { signals.add(Signal.CALL); core++ }

        val s = when {
            link && core >= 1 -> maxOf(p, 0.92)
            core >= 2 -> maxOf(p, 0.85)
            core == 1 -> minOf(0.99, p + 0.25)
            link -> minOf(0.99, p + 0.25)
            TXN.containsMatchIn(t) -> minOf(p, 0.25)   // normal bank/recharge/delivery alert
            else -> p
        }
        val level = when {
            s >= 0.75 -> RiskLevel.HIGH
            s >= 0.5 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
        return SmsResult(p, s, signals, level)
    }

    companion object {
        private val TOKEN: Pattern = Pattern.compile("\\b\\w+\\b")

        private fun rx(p: String) = Regex(p)
        private val URL = rx("""https?://\S+|www\.\S+|\b[a-z0-9-]+\.(?:xyz|top|cc|club|click|site|online|buzz|live|icu|vip|info|cfd|sbs)\b""")
        private val SHORT = rx("""\b(?:bit\.ly|tinyurl\.com|goo\.gl|t\.co|cutt\.ly|rb\.gy|is\.gd|t\.me)\b""")
        private val URG = rx("""\burgent\b|\bimmediately\b|\bturant\b|\bright now\b|within \d+ ?(?:hours|hrs|hour|mins|minutes)|will be (?:blocked|suspended|disconnected|deactivated|frozen)|disconnected|block ho|blocked|suspended|\bfrozen\b|\bfreeze\b|kat jayega|kat jaayega|band hogi|band ho jayega|deactivated|last chance|final notice|unauthori[sz]ed|expire ho|has expired|account kal|24 hours""")
        private val KYC = rx("""\bkyc\b|pan card|pan update|aadhaar link|aadhar link|aadhaar nahi""")
        private val OTPW = rx("""do not share|don't share|never share|do not disclose|na karein|mat batao|kisi ke saath share|share na""")
        private val OTPR = rx("""(?:share|send|tell|bata\w*|bhej\w*|give)\b.{0,25}\b(?:otp|pin|cvv|code)\b|\b(?:otp|pin|cvv)\b.{0,15}(?:bata|bhej|share|send)\w*""")
        private val PRIZE = rx("""\bwon\b|\bwinner\b|lottery|\blakh\b|\bcrore\b|\binaam\b|congratulations|selected for|\biphone\b|free recharge|guaranteed returns|invest rs""")
        private val JOB = rx("""work from home|ghar baithe|part time|earn rs|daily income|kamayein|task job|per task|telegram|easy task""")
        private val MONEY = rx("""processing fee|registration fee|customs|pay rs \d+|fee at|refund (?:ke liye )?(?:link|karne)|galti se|anydesk|teamviewer|send your bank|bank account number|arrest|police notice|case registered|loan approved|no documents""")
        private val CALL = rx("""call (?:now|immediately|us)|call\s*[:\-]?\s*\+?\d{10}|\bcall\b.{0,20}\d{10}|whatsapp.{0,15}\d{10}""")
        private val TXN = rx("""debited|credited|debit hue|credit hue|recharge of|recharge successful|out for delivery|has been delivered|\bpnr\b|your otp|otp is|is your otp|otp for|has shipped|has been processed|statement of|is due on|appointment|arriving""")

        private fun sigmoid(z: Double) = 1.0 / (1.0 + exp(-z))

        /** Loads assets/sms_model.json. Call off the main thread (file is a few MB). */
        fun load(context: Context): SmsDetector {
            val json = context.assets.open("sms_model.json").bufferedReader().use { it.readText() }
            val o = JSONObject(json)
            val v = o.getJSONObject("vocab")
            val vocab = HashMap<String, Int>(v.length() * 2)
            val keys = v.keys()
            while (keys.hasNext()) { val k = keys.next(); vocab[k] = v.getInt(k) }
            val idfA = o.getJSONArray("idf")
            val coefA = o.getJSONArray("coef")
            val idf = DoubleArray(idfA.length()) { idfA.getDouble(it) }
            val coef = DoubleArray(coefA.length()) { coefA.getDouble(it) }
            return SmsDetector(vocab, idf, coef, o.getDouble("intercept"))
        }
    }
}

/**
 * Everything about what the user sees. Design choice: a 1B LLM only writes ONE summary
 * sentence; the safety-critical advice (what to do) is fixed, hard-coded and always correct.
 */
object SmsAdvisor {

    /** Fixed remediation actions. Never generated by the LLM. */
    val ACTIONS_SCAM = listOf(
        "Link par click mat karo",
        "Bank/company ke official app ya number se khud verify karo",
        "OTP, PIN, password kisi ko mat batao",
        "Cyber crime helpline 1930 par report karo"
    )
    val ACTIONS_SAFE = listOf(
        "Phir bhi koi link ya OTP kisi ko mat do"
    )

    fun actions(r: SmsResult): List<String> = if (r.isSuspicious) ACTIONS_SCAM else ACTIONS_SAFE

    /** Prompt for Llama. Only used for flagged SMS; the answer start is given via prefill(). */
    fun buildPrompt(sms: String, r: SmsResult): String {
        val reasons = if (r.signals.isEmpty()) "looks suspicious overall"
        else r.signals.joinToString(", ") { it.reason }
        val clean = sms.replace("\"", "'").replace("\n", " ").take(200)
        return """Explain in ONE short Hinglish sentence why this SMS was flagged. Use only the given reasons.

SMS: "Your KYC expired, click bit.ly/xyz now"
Reasons: asks for urgent action, contains a suspicious or shortened link, asks for KYC update
Explanation: Ye message scam ho sakta hai kyunki ye jaldi karne ka dabav bana raha hai aur KYC maang raha hai.

SMS: "You won Rs 10 lakh! Call 9999999999 now"
Reasons: promises a prize or easy money, asks to call or message a number
Explanation: Ye message scam ho sakta hai kyunki ye inaam ka lalach de raha hai aur number par call karne ko keh raha hai.

SMS: "$clean"
Reasons: $reasons
Explanation:"""
    }

    /** Fixed first words of the answer. Llama only completes the reason after "kyunki". */
    fun prefill(r: SmsResult): String =
        if (r.level == RiskLevel.HIGH) "Ye message scam ho sakta hai kyunki"
        else "Ye message shak wala lag raha hai kyunki"

    private val BANNED = listOf(
        "click karo", "click karein", "visit karo", "visit karein", "link kholo",
        "link par click", "otp batao", "otp bata do", "share karo", "download", "install"
    )

    /** Fixed template line, used whenever the LLM output is not safe to show. */
    fun fallbackSummary(r: SmsResult): String {
        if (!r.isSuspicious) return "Ye message normal lag raha hai, koi bada khatra nahi dikh raha."
        val why = r.signals.take(3).joinToString(", ") { it.hinglish }
        return if (why.isEmpty()) "Ye message scam ho sakta hai, savdhan rahein."
        else "Ye message $why, isliye ye scam ho sakta hai."
    }

    /**
     * Safety check on Llama's continuation. Returns the full sentence if it is clean AND consistent
     * with the detected signals, otherwise null (caller then uses the fixed template).
     */
    fun checkLlama(llmOutput: String?, r: SmsResult): String? {
        if (!r.isSuspicious) return null                 // safe SMS: fixed line, no LLM
        val cont = (llmOutput ?: "").trim()
        if (cont.isEmpty()) return null
        val end = cont.indexOfFirst { it == '.' || it == '!' || it == '?' || it == '\n' }
        val clause = (if (end >= 0) cont.substring(0, end) else cont).trim()
        if (clause.length < 6 || clause.length > 140) return null
        val low = clause.lowercase(Locale.ROOT)
        if (BANNED.any { low.contains(it) }) return null
        // a flagged SMS must not be explained as normal/safe
        if (low.contains("normal") || low.contains("safe") || low.contains("khatra nahi")) return null
        if (clause.contains("\"") || low.startsWith("sms")) return null
        // every claim in the sentence must be backed by a real detected signal
        val has = r.signals.toSet()
        fun mentions(vararg w: String) = w.any { low.contains(it) }
        if (mentions("kyc") && Signal.KYC !in has) return null
        if (mentions("link", "website") && Signal.LINK !in has) return null
        if (mentions("jaldi", "turant", "urgent") && Signal.URGENCY !in has) return null
        if (mentions("otp", "pin", "password") && Signal.OTP_REQUEST !in has) return null
        if (mentions("inaam", "lottery", "jeet") && Signal.PRIZE !in has) return null
        if (mentions("job", "naukri", "kaam") && Signal.JOB !in has) return null
        if (mentions("call", "number", "whatsapp") && Signal.CALL !in has) return null
        if (mentions("fees", "fee", "paise bhej", "payment") && Signal.MONEY !in has) return null
        return prefill(r) + " " + clause + "."
    }

    fun safeSummary(llmOutput: String?, r: SmsResult): String =
        checkLlama(llmOutput, r) ?: fallbackSummary(r)
}
