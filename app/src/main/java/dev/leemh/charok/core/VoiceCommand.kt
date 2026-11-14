package dev.leemh.charok.core

sealed interface VoiceCommand {
    data object Start : VoiceCommand
    data object Stop : VoiceCommand
    data object Mark : VoiceCommand
    data class Description(val text: String) : VoiceCommand
}
/** Deliberately narrow: an arbitrary symptom sentence containing '종료' is not a stop command. */
fun parseVoice(text: String): VoiceCommand {
    val normalized = text.trim().removeSuffix(".").replace(" ", "").removePrefix("차록")
    return when (normalized) {
        "기록해줘", "증상기록해줘", "기록시작", "기록시작해줘" -> VoiceCommand.Start
        "종료해줘", "기록종료해줘", "기록종료", "기록중지", "기록멈춰줘" -> VoiceCommand.Stop
        "지금이야", "지금", "방금또났어", "방금또났어요" -> VoiceCommand.Mark
        else -> VoiceCommand.Description(text.take(1000))
    }
}
