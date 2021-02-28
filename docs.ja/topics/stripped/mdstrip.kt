import kotlin.text.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.nio.file.*

class Input(filePath: Path) {
    val lines: List<String> = Files.readAllLines(filePath)
}

private fun String.isBlankLine(): Boolean {
    val regexBLANKLINE = Regex("""^\s*$""")
    return regexBLANKLINE.matches(this)
}

private fun String.scanXMLComment(): Triple<String?, String?, String?> {
    val regexONELINE = Regex("""^(.*?)<!--(.*?)-->(.*)$""")
    val regexBEGLINE = Regex("""^(.*?)<!--(.*)$""")
    val regexENDLINE = Regex("""^(.*?)-->(.*)$""")
    val matchOneLine = regexONELINE.find(this)
    if (matchOneLine != null) {
        val groups = matchOneLine.groups
        assert((groups[1] != null) && (groups[2] != null) && (groups[3] != null))
        return Triple(groups[1]?.value, groups[2]?.value, groups[3]?.value) // 111
    }
    val matchBegLine = regexBEGLINE.find(this)
    if (matchBegLine != null) {
        val groups = matchBegLine.groups
        assert((groups[1] != null) && (groups[2] != null))
        return Triple(groups[1]?.value, groups[2]?.value, null) // 110
    }
    val matchEndLine = regexENDLINE.find(this)
    if (matchEndLine != null) {
        val groups = matchEndLine.groups
        assert((groups[1] != null) && (groups[2] != null))
        return Triple(null, groups[1]?.value, groups[2]?.value) // 011
    }
    return Triple(this, null, null) // 100
}

fun Flow<String>.removeComments(): Flow<String> = flow {

    suspend fun whenInNormalLine(line: String): Boolean {
        if (line.isBlankLine()) {
            emit("")
            return false
        }
        val (prec, comm, succ) = line.scanXMLComment() // (P)<!--(C)-->(S), (P)<!--(C), (P), (C)-->(S)
        if (prec != null) { // (P)<!--(C)-->(S), (P)<!--(C), (P)
            emit(prec)
            if (comm == null) return false // (P)
            if (succ == null) return true  // (P)<!--(C)
            return whenInNormalLine(succ) // (P)<!--(C)-->(S)
        } else { // (C)-->(S) ill-formed
            if ((comm == null) || (succ == null)) return false // never occurs
            emit("$comm-->$succ")
            return false
        }
    }

    suspend fun whenInCommentLine(line: String): Boolean {
        if (line.isBlankLine()) return true
        val (_, _, succ) = line.scanXMLComment()
        if (succ == null) return true // (P)<!--(C) ill-formed, (P)
        return whenInNormalLine(succ) // (P)<!--(C)-->(S) ill-formed, (C)-->(S)
    }

    var inComment = false
    collect { line ->
        inComment = if ( !(inComment) ) whenInNormalLine(line) else whenInCommentLine(line)
    }
}

    fun Flow<String>.removeConsecutiveBlankLines(): Flow<String> = flow {
    var linePrev: String? = null
    collect { line: String ->
        if ( (linePrev == null) || !(linePrev!!.isBlankLine()) || !(line.isBlankLine()) ) emit(line)
        linePrev = line
    }
}

enum class LineLabels {
    BLANK, TEXT, TITLE, TITLESEP, CODESEP, QUOTE, LIST, SEPLINE, LINK
}

fun Flow<String>.labelLines(): Flow< Pair<LineLabels, String> > = flow {
    val regexBLANK    = Regex("""^\s*$""")
    val regexTITLE    = Regex("""^\s*#.*$""")
    val regexTITLESEP = Regex("""^(?:=+|-+)\s*$""")
    val regexCODESEP  = Regex("""^```.*$""")
    val regexQUOTE    = Regex("""^>.*$""")
    val regexLIST     = Regex("""^(?:\s+)?(?:-|\*|[0-9]+\.).*$""")
    val regexSEPLINE  = Regex("""^(?:\s*\*\s*\*\s*\*[*\s]*|\s*_\s*_\s*_[_\s]*|\s*-\s*-\s*-[\-\s]*)$""")
    val regexLINK     = Regex("""^\[.*?]:.*$""")
    collect { line ->
        val label = when {
            (regexBLANK.matches(line))    -> LineLabels.BLANK
            (regexTITLE.matches(line))    -> LineLabels.TITLE
            (regexTITLESEP.matches(line)) -> LineLabels.TITLESEP
            (regexCODESEP.matches(line))  -> LineLabels.CODESEP
            (regexQUOTE.matches(line))    -> LineLabels.QUOTE
            (regexLIST.matches(line))     -> LineLabels.LIST
            (regexSEPLINE.matches(line))  -> LineLabels.SEPLINE
            (regexLINK.matches(line))     -> LineLabels.LINK
            else                          -> LineLabels.TEXT
        }
        emit(Pair(label, line))
    }
}

fun Flow< Pair<LineLabels, String> >.concatParagraphLines(): Flow<String> = flow {
    val regexHALFWIDTH = Regex("""^[!-~].*$""")
    var inCodeBlock = false
    var labelPrev: LineLabels? = null
    var linePrev:  String?     = null
    collect { (labelCurr, lineCurr) ->
        if (labelCurr == LineLabels.CODESEP) inCodeBlock = !inCodeBlock
        if ( (labelPrev != null) && (linePrev != null) ) {
            if ( (labelPrev == LineLabels.TEXT) && (labelCurr == LineLabels.TEXT) && (!inCodeBlock) ) {
                if (regexHALFWIDTH.matches(lineCurr)) emit("$linePrev ") else emit("$linePrev")
            } else {
                emit("$linePrev\n")
            }
        }
        labelPrev = labelCurr
        linePrev  = lineCurr
    }
    emit("$linePrev\n")
}

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        System.err.println("mdstrip <filepath>\n")
        return
    }
    val input = Input(Paths.get(args[0]))
    runBlocking {
        coroutineScope {
            input.lines.asFlow()
                .removeComments()
                .removeConsecutiveBlankLines()
                .labelLines().concatParagraphLines()
                .collect { line -> print(line) }
        }
    }
}
