package com.taptoneluthier.recorder

/**
 * Représente un bloc de données audio capturé depuis le microphone.
 *
 * Cette classe regroupe les informations produites lors d'une lecture
 * de l'enregistreur natif : les données PCM brutes, la quantité réelle
 * de données lues, ainsi que quelques métadonnées utiles au traitement
 * et au débogage.
 *
 * Chaque instance correspond à une portion courte du signal audio
 * pouvant ensuite être transmise vers la couche supérieure pour
 * l'analyse, l'affichage ou l'enregistrement.
 *
 * @property sampleRate Taux d'échantillonnage du bloc, en hertz.
 * @property bytesRead Nombre réel d'octets lus dans le tampon natif.
 * @property samples Nombre d'échantillons contenus dans ce bloc.
 * @property pcmBytes Données audio brutes au format PCM 16 bits.
 * @property peak Valeur de crête détectée dans ce bloc.
 * @property timestampMs Horodatage du bloc en millisecondes.
 */
data class AudioChunk(
    val sampleRate: Int,
    val bytesRead: Int,
    val samples: Int,
    val pcmBytes: ByteArray,
    val peak: Int,
    val timestampMs: Long
)