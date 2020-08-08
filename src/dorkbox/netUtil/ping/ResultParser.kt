package dorkbox.netUtil.ping

import java.util.regex.Matcher
import java.util.regex.Pattern

class ResultParser(private val pattern: Pattern, private val reader: (PingResult, Matcher) -> PingResult) {
    fun fill(result: PingResult, output: String): PingResult {
        var result = result
        val matcher = pattern.matcher(output)
        while (matcher.find()) {
            result = reader(result, matcher)
        }

        return result
    }

    companion object {
        fun of(regex: String, reader: (PingResult, Matcher) -> PingResult): ResultParser {
            val compile = Pattern.compile(regex)
            return ResultParser(compile, reader)
        }
    }
}
