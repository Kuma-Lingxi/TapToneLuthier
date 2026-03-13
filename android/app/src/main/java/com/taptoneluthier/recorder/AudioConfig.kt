package com.taptoneluthier.recorder

import android.media.AudioFormat


/**
 * Définit les constantes de configuration audio utilisées par le module
 * d'enregistrement natif.
 *
 * Cet objet centralise les paramètres techniques nécessaires à
 * l'initialisation de {@code AudioRecord}, notamment le taux
 * d'échantillonnage, le format PCM, la configuration des canaux,
 * la taille des blocs de lecture et la source audio.
 *
 * Dans TapTone Luthier, cette configuration sert à garantir une capture
 * cohérente du signal microphone avant son transfert vers la couche
 * d'analyse.
 */
object AudioConfig {
    /**
     * Taux d'échantillonnage utilisé pour la capture audio, en hertz.
     */
    const val SAMPLE_RATE = 44100

    /**
     * Configuration des canaux d'entrée.
     *
     * Ici, l'enregistrement est effectué en mono.
     */
    const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO

    /**
     * Format des échantillons audio capturés.
     *
     * Ici, les données sont lues en PCM signé sur 16 bits.
     */
    const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    /**
     * Nombre de canaux audio utilisés par le flux d'entrée.
     */
    const val CHANNEL_COUNT = 1

    /**
     * Nombre d'octets utilisés pour représenter un échantillon audio.
     */
    const val BYTES_PER_SAMPLE = 2

    /**
     * Taille d'un bloc audio exprimée en nombre d'échantillons.
     */
    const val CHUNK_SIZE_IN_SAMPLES = 2048

    /**
     * Taille d'un bloc audio exprimée en octets.
     */
    const val CHUNK_SIZE_IN_BYTES = CHUNK_SIZE_IN_SAMPLES * BYTES_PER_SAMPLE

    /**
     * Source audio utilisée pour la capture.
     *
     * Ici, le signal provient du microphone de l'appareil.
     */
    const val AUDIO_SOURCE = android.media.MediaRecorder.AudioSource.MIC
}