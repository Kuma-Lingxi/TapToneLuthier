package com.taptoneluthier.bridge

import android.Manifest
import android.util.Base64
import androidx.annotation.RequiresPermission
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.taptoneluthier.recorder.AudioChunk
import com.taptoneluthier.recorder.AudioRecorderService
import com.taptoneluthier.recorder.RecorderException

/**
 * Module natif React Native servant de pont entre la couche JavaScript
 * et le service Android de capture audio {@link AudioRecorderService}.
 *
 * Cette classe expose à JavaScript les opérations principales liées
 * à l'enregistrement audio :
 * - démarrer la capture ;
 * - arrêter la capture ;
 * - interroger l'état courant de l'enregistrement.
 *
 * Elle implémente également {@link AudioRecorderService.Listener} afin de
 * recevoir les événements produits par le service natif, puis de les
 * retransmettre à la couche JS sous forme d'événements React Native.
 *
 * Responsabilités :
 * - adapter les appels JS en appels Kotlin vers {@link AudioRecorderService} ;
 * - convertir les résultats synchrones et les erreurs en objets {@link Promise} ;
 * - convertir les blocs audio {@link AudioChunk} en données sérialisables
 *   pour le bridge React Native ;
 * - émettre des événements JS pour les blocs PCM, les erreurs et les
 *   changements d'état d'enregistrement ;
 * - assurer la libération correcte des ressources natives lors de
 *   l'invalidation du module.
 *
 * Détails de communication avec JavaScript :
 * - `startRecording` : démarre la capture ;
 * - `stopRecording` : arrête la capture ;
 * - `isRecording` : retourne l'état courant ;
 * - événement `audioChunk` : transmet un bloc PCM encodé en Base64 ainsi
 *   que ses métadonnées ;
 * - événement `recorderError` : transmet une erreur de capture ;
 * - événement `recorderStateChanged` : notifie un changement d'état.
 *
 * Hors périmètre :
 * - analyse fréquentielle ;
 * - calcul FFT ;
 * - détection de la fréquence dominante ;
 * - logique métier de mesure acoustique.
 *
 * Le rôle de ce module est strictement celui d'un adaptateur de bridge
 * entre React Native et la couche native Android.
 *
 * @param reactContext contexte React Native utilisé pour accéder au bridge
 * et émettre des événements vers JavaScript.
 */


class TapToneRecorderModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), AudioRecorderService.Listener {

    private val appContext = reactApplicationContext
    private val audioRecorderService: AudioRecorderService = AudioRecorderService(appContext)

    init {
        audioRecorderService.setListener(this)
    }

    /**
     * Retourne le nom public du module natif exposé à React Native.
     *
     * Ce nom est utilisé côté JavaScript pour récupérer le module via
     * le bridge React Native.
     *
     * @return nom du module natif enregistré auprès de React Native.
     */
    override fun getName(): String = "TapToneRecorder"


    /**
     * Démarre la capture audio native.
     *
     * Cette méthode est exposée à la couche JavaScript via le bridge
     * React Native. En cas de succès, la promesse est résolue sans valeur.
     * En cas d'échec, la promesse est rejetée avec un code d'erreur adapté.
     *
     * Après un démarrage réussi, les blocs audio capturés sont transmis
     * de manière asynchrone via l'événement `audioChunk`.
     *
     * @param promise promesse React Native résolue lorsque le démarrage
     * est effectué, ou rejetée si l'initialisation échoue.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @ReactMethod
    fun startRecording(promise: Promise) {
        try {
            audioRecorderService.demarrer()
            promise.resolve(null)
        } catch (e: RecorderException) {
            promise.reject("RECORDER_START_ERROR", e.message, e)
        } catch (e: Exception) {
            promise.reject("UNEXPECTED_START_ERROR", e.message, e)
        }
    }

    /**
     * Arrête la capture audio en cours.
     *
     * Cette méthode est exposée à JavaScript via React Native.
     * Lorsque l'arrêt se déroule correctement, la promesse est résolue.
     * En cas d'erreur, la promesse est rejetée.
     *
     * @param promise promesse React Native résolue lorsque l'arrêt est
     * terminé, ou rejetée si une erreur survient pendant l'arrêt.
     */
    @ReactMethod
    fun stopRecording(promise: Promise) {
        try {
            audioRecorderService.arreter()
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("RECORDER_STOP_ERROR", e.message, e)
        }
    }

    /**
     * Indique si une capture audio est actuellement active.
     *
     * Cette méthode est exposée à JavaScript et permet à la couche
     * React Native d'interroger l'état courant du service natif.
     *
     * @param promise promesse React Native résolue avec `true` si un
     * enregistrement est actif, sinon `false`.
     */
    @ReactMethod
    fun isRecording(promise: Promise) {
        try {
            promise.resolve(audioRecorderService.isRecording())
        } catch (e: Exception) {
            promise.reject("RECORDER_STATE_ERROR", e.message, e)
        }
    }

    /**
     * Reçoit un bloc audio brut produit par le service natif et le
     * retransmet à la couche JavaScript sous forme d'événement React Native.
     *
     * Le contenu PCM est sérialisé en Base64 afin d'être transporté à
     * travers le bridge React Native avec ses métadonnées de capture.
     *
     * @param chunk bloc audio capturé par {@link AudioRecorderService}.
     */
    override fun onAudioChunk(chunk: AudioChunk) {
        val params = Arguments.createMap().apply {
            putInt("sampleRate", chunk.sampleRate)
            putInt("bytesRead", chunk.bytesRead)
            putInt("samples", chunk.samples)
            putInt("peak", chunk.peak)
            putDouble("timestampMs", chunk.timestampMs.toDouble())
            putString(
                "pcmBase64",
                Base64.encodeToString(chunk.pcmBytes, Base64.NO_WRAP)
            )
        }

        sendEvent("audioChunk", params)
    }

    /**
     * Reçoit une erreur de capture audio provenant du service natif et
     * la propage à JavaScript via l'événement `recorderError`.
     *
     * @param error erreur métier décrivant le problème rencontré durant
     * l'initialisation ou la capture audio.
     */
    override fun onError(error: RecorderException) {
        val params = Arguments.createMap().apply {
            putString("message", error.message ?: "Erreur inconnue")
        }
        sendEvent("recorderError", params)
    }

    /**
     * Reçoit un changement d'état d'enregistrement et le notifie à la
     * couche JavaScript via un événement React Native.
     *
     * @param estEnregistrementActif `true` si la capture est active,
     * `false` sinon.
     */
    override fun onRecordingStateChanged(estEnregistrementActif: Boolean) {
        val params = Arguments.createMap().apply {
            putBoolean("isRecording", estEnregistrementActif)
        }
        sendEvent("recorderStateChanged", params)
    }

    /**
     * Libère les ressources détenues par le module natif lors de son
     * invalidation par React Native.
     *
     * Cette méthode retire le listener enregistré auprès du service de
     * capture, libère les ressources audio natives, puis délègue la fin
     * du cycle de vie à l'implémentation parente.
     */
    override fun invalidate() {
        audioRecorderService.setListener(null)
        audioRecorderService.liberer()
        super.invalidate()
    }

    private fun sendEvent(eventName: String, params: com.facebook.react.bridge.WritableMap) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }
}