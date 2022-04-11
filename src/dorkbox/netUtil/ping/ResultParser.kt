/*
 * Copyright 2022 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dorkbox.netUtil.ping

import java.util.regex.*

class ResultParser(private val pattern: Pattern, private val reader: (PingResult, Matcher) -> PingResult) {
    fun fill(result: PingResult, output: String): PingResult {
        var r = result
        val matcher = pattern.matcher(output)
        while (matcher.find()) {
            r = reader(r, matcher)
        }

        return r
    }

    companion object {
        fun of(regex: String, reader: (PingResult, Matcher) -> PingResult): ResultParser {
            val compile = Pattern.compile(regex)
            return ResultParser(compile, reader)
        }
    }
}
