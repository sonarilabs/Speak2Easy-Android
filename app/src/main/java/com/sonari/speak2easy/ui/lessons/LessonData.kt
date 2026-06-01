package com.sonari.speak2easy.ui.lessons

/** Static lesson definitions ported 1:1 from iOS `Lesson.hiraganaLessons` / `katakanaLessons`. */
object LessonData {

    val hiraganaLessons: List<LessonGroup> = listOf(
        LessonGroup(
            title = "Basic Hiragana",
            subtitle = "Single Characters",
            characterSet = JapaneseScript.HIRAGANA,
            contentType = ContentKind.SINGLE_CHARACTER,
            lessons = listOf(
                LessonInfo(1, "Vowels", "あ い う え お", listOf("あ", "い", "う", "え", "お")),
                LessonInfo(2, "K Row", "か き く け こ", listOf("か", "き", "く", "け", "こ")),
                LessonInfo(3, "S Row", "さ し す せ そ", listOf("さ", "し", "す", "せ", "そ")),
                LessonInfo(4, "T Row", "た ち つ て と", listOf("た", "ち", "つ", "て", "と")),
                LessonInfo(5, "N Row", "な に ぬ ね の", listOf("な", "に", "ぬ", "ね", "の")),
                LessonInfo(6, "H Row", "は ひ ふ へ ほ", listOf("は", "ひ", "ふ", "へ", "ほ")),
                LessonInfo(7, "M Row", "ま み む め も", listOf("ま", "み", "む", "め", "も")),
                LessonInfo(8, "Y Row", "や ゆ よ", listOf("や", "ゆ", "よ")),
                LessonInfo(9, "R Row", "ら り る れ ろ", listOf("ら", "り", "る", "れ", "ろ")),
                LessonInfo(10, "W Row & N", "わ を ん", listOf("わ", "を", "ん")),
            ),
        ),
        LessonGroup(
            title = "Dakuten & Handakuten",
            subtitle = "Voiced Characters",
            characterSet = JapaneseScript.HIRAGANA,
            contentType = ContentKind.SINGLE_CHARACTER,
            lessons = listOf(
                LessonInfo(11, "G Row", "が ぎ ぐ げ ご", listOf("が", "ぎ", "ぐ", "げ", "ご")),
                LessonInfo(12, "Z Row", "ざ じ ず ぜ ぞ", listOf("ざ", "じ", "ず", "ぜ", "ぞ")),
                LessonInfo(13, "D Row", "だ ぢ づ で ど", listOf("だ", "ぢ", "づ", "で", "ど")),
                LessonInfo(14, "B Row", "ば び ぶ べ ぼ", listOf("ば", "び", "ぶ", "べ", "ぼ")),
                LessonInfo(15, "P Row", "ぱ ぴ ぷ ぺ ぽ", listOf("ぱ", "ぴ", "ぷ", "ぺ", "ぽ")),
            ),
        ),
        LessonGroup(
            title = "Combinations",
            subtitle = "Double Characters",
            characterSet = JapaneseScript.HIRAGANA,
            contentType = ContentKind.DOUBLE_CHARACTER,
            lessons = listOf(
                LessonInfo(16, "Y Combinations", "きゃ ぎょ しょ じゅ...", listOf("きゃ", "きゅ", "きょ", "ぎゃ", "ぎゅ", "ぎょ", "しゃ", "しゅ", "しょ", "じゃ", "じゅ")),
                LessonInfo(17, "Y Combinations", "じょ ちょ にょ ひょ びゃ...", listOf("じょ", "ちゃ", "ちゅ", "ちょ", "にゃ", "にゅ", "にょ", "ひゃ", "ひゅ", "ひょ", "びゃ")),
                LessonInfo(18, "Y Combinations", "びゅ ぴょ みょ りょ...", listOf("びゅ", "びょ", "ぴゃ", "ぴゅ", "ぴょ", "みゃ", "みゅ", "みょ", "りゃ", "りゅ", "りょ")),
            ),
        ),
    )

    val katakanaLessons: List<LessonGroup> = listOf(
        LessonGroup(
            title = "Basic Katakana",
            subtitle = "Single Characters",
            characterSet = JapaneseScript.KATAKANA,
            contentType = ContentKind.SINGLE_CHARACTER,
            lessons = listOf(
                LessonInfo(1, "Vowels", "ア イ ウ エ オ", listOf("ア", "イ", "ウ", "エ", "オ")),
                LessonInfo(2, "K Row", "カ キ ク ケ コ", listOf("カ", "キ", "ク", "ケ", "コ")),
                LessonInfo(3, "S Row", "サ シ ス セ ソ", listOf("サ", "シ", "ス", "セ", "ソ")),
                LessonInfo(4, "T Row", "タ チ ツ テ ト", listOf("タ", "チ", "ツ", "テ", "ト")),
                LessonInfo(5, "N Row", "ナ ニ ヌ ネ ノ", listOf("ナ", "ニ", "ヌ", "ネ", "ノ")),
                LessonInfo(6, "H Row", "ハ ヒ フ ヘ ホ", listOf("ハ", "ヒ", "フ", "ヘ", "ホ")),
                LessonInfo(7, "M Row", "マ ミ ム メ モ", listOf("マ", "ミ", "ム", "メ", "モ")),
                LessonInfo(8, "Y Row", "ヤ ユ ヨ", listOf("ヤ", "ユ", "ヨ")),
                LessonInfo(9, "R Row", "ラ リ ル レ ロ", listOf("ラ", "リ", "ル", "レ", "ロ")),
                LessonInfo(10, "W Row & N", "ワ ヲ ン", listOf("ワ", "ヲ", "ン")),
            ),
        ),
        LessonGroup(
            title = "Dakuten & Handakuten",
            subtitle = "Voiced Characters",
            characterSet = JapaneseScript.KATAKANA,
            contentType = ContentKind.SINGLE_CHARACTER,
            lessons = listOf(
                LessonInfo(11, "G Row", "ガ ギ グ ゲ ゴ", listOf("ガ", "ギ", "グ", "ゲ", "ゴ")),
                LessonInfo(12, "Z Row", "ザ ジ ズ ゼ ゾ", listOf("ザ", "ジ", "ズ", "ゼ", "ゾ")),
                LessonInfo(13, "D Row", "ダ ヂ ヅ デ ド", listOf("ダ", "ヂ", "ヅ", "デ", "ド")),
                LessonInfo(14, "B Row", "バ ビ ブ ベ ボ", listOf("バ", "ビ", "ブ", "ベ", "ボ")),
                LessonInfo(15, "P Row", "パ ピ プ ペ ポ", listOf("パ", "ピ", "プ", "ペ", "ポ")),
            ),
        ),
        LessonGroup(
            title = "Combinations",
            subtitle = "Double Characters",
            characterSet = JapaneseScript.KATAKANA,
            contentType = ContentKind.DOUBLE_CHARACTER,
            lessons = listOf(
                LessonInfo(16, "Y Combinations", "キャ ギョ ショ ジュ...", listOf("キャ", "キュ", "キョ", "ギャ", "ギュ", "ギョ", "シャ", "シュ", "ショ", "ジャ", "ジュ")),
                LessonInfo(17, "Y Combinations", "ジョ チョ ニョ ヒョ ビャ...", listOf("ジョ", "チャ", "チュ", "チョ", "ニャ", "ニュ", "ニョ", "ヒャ", "ヒュ", "ヒョ", "ビャ")),
                LessonInfo(18, "Y Combinations", "ビュ ピョ ミョ リョ...", listOf("ビュ", "ビョ", "ピャ", "ピュ", "ピョ", "ミャ", "ミュ", "ミョ", "リャ", "リュ", "リョ")),
            ),
        ),
    )
}
