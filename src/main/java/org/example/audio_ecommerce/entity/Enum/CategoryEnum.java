package org.example.audio_ecommerce.entity.Enum;

/**
 * Danh mục sản phẩm chính, tương ứng với nhóm thuộc tính kỹ thuật trong Product.
 */
public enum CategoryEnum {

    // 🔊 LOA
    SPEAKER,          // Có: driverConfiguration, driverSize, frequencyResponse, impedance, powerHandling...

    // 🎤 MICRO
    MICROPHONE,       // Có: micType, polarPattern, maxSPL, micSensitivity...

    // 📻 AMPLIFIER / RECEIVER
    AMPLIFIER,        // Có: amplifierType, totalPowerOutput, thd, snr, input/outputChannels...

    // 🎛️ MIXER / DJ / EQ
    MIXER,            // Có: channelCount, eqBands, hasPhantomPower, builtInEffects, midiSupport...

    // 🎧 TAI NGHE
    HEADPHONE,        // Có: headphoneType, driverType, noiseCancelling...

    // 🎚️ DAC / SOUND CARD
    DAC,              // Có: dacChipset, sampleRate, bitDepth, input/outputInterface...

    // 🔌 DÂY KẾT NỐI
    CABLE,            // Có: cableType, cableLength, connectorType...

    // 💿 ĐẦU ĐĨA CD
    CD_PLAYER,        // Có: mechanismType, supportSACD...

    // 📀 MÂM ĐĨA THAN
    TURNTABLE         // Có: platterMaterial, motorType, tonearmType, autoReturn...
}
