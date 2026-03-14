package com.taptoneluthier.recorder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.taptoneluthier.common.contrat.invariant
import com.taptoneluthier.common.contrat.postcondition
import com.taptoneluthier.common.contrat.precondition


/**
 * Service de capture audio temps réel basé sur {@link AudioRecord}.
 *
 * Cette classe gère le cycle de vie de l'enregistreur natif Android :
 * création, démarrage, lecture continue des blocs PCM, arrêt et libération
 * des ressources associées au microphone.
 *
 * Elle produit des objets {@link AudioChunk} transmis au {@link Listener}
 * à mesure que les données audio sont capturées.
 *
 * Responsabilités :
 * - acquérir des échantillons PCM 16 bits mono ;
 * - découper le flux audio en blocs ;
 * - notifier les changements d'état d'enregistrement ;
 * - remonter les erreurs de capture sous forme de {@link RecorderException}.
 *
 * Hors périmètre :
 * - analyse fréquentielle ;
 * - estimation de hauteur ou de fréquence dominante ;
 * - interprétation métier du signal audio.
 *
 * Cycle de vie typique :
 * 1. créer une instance du service ;
 * 2. enregistrer un {@link Listener} ;
 * 3. appeler {@link #demarrer()} ;
 * 4. recevoir les callbacks audio ;
 * 5. appeler {@link #arreter()} ou {@link #liberer()}.
 */
class AudioRecorderService(
    private val context: Context
) {
    //Properties
    private val appContext : Context = context.applicationContext

    private  var audioRecord: AudioRecord? = null
    private  var recordingThread :Thread? = null

    @Volatile
    private var estEnregistrementActif : Boolean = false
    @Volatile
    private  var listener : Listener? = null
    private var bufferSizeInBytes :Int = 0

    // API

    /**
     * Démarre la capture audio et lance la lecture asynchrone des blocs PCM.
     *
     * Après un démarrage réussi, le service entre dans un état actif et commence
     * à transmettre des {@link AudioChunk} au {@link Listener} enregistré.
     *
     * Préconditions :
     * - la permission {@code RECORD_AUDIO} doit être accordée ;
     * - aucun enregistrement ne doit déjà être en cours.
     *
     * Effets :
     * - initialise l'instance {@link AudioRecord} ;
     * - démarre l'enregistrement natif ;
     * - lance le thread de lecture ;
     * - notifie le listener du passage à l'état actif.
     *
     * @throws RecorderException si l'initialisation ou le démarrage de
     * l'enregistreur échoue.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Throws(RecorderException::class)
    fun demarrer() {
        verifieInvariant()

        // 0 - Vérifier la permission microphone
        precondition(avoirRecorderAudioPermission()) {
            "avoirRecorderAudioPermission()"
        }
        // 0 - Vérifier si un enregistrement est déjà en cours.
        precondition(!estEnregistrementActif) {
            "!estRecorder"
        }

        try {
            //1 - Calculer la taille du tampon valide(Buffer size)
            bufferSizeInBytes = calculeBufferSizeInBytes()
            postcondition(bufferSizeInBytes >= AudioConfig.CHUNK_SIZE_IN_BYTES) {
                "bufferSizeInBytes >= AudioConfig.CHUNK_SIZE_IN_BYTES"
            }

            //2 - Créer une instance AudioRecord
            val recorder = creerAudioRecord(bufferSizeInBytes)

            //3 - Vérifier que AudioRecord a été initialisé avec succès.
            if(recorder.state != AudioRecord.STATE_INITIALIZED){
                recorder.release()
                throw RecorderException("AudioRecord n'a pas pu être initialisé correctement.")
            }

            //4 - Démarrer l'enregistrement natif
            recorder.startRecording()

            //5 - Vérifier que l'état de l'enregistrement est bien passé à RECORDSTATE_RECORDING.
            if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                recorder.release()
                throw RecorderException("Le démarrage de l'enregistrement a échoué.")
            }

            //6 - Jusqu'ici, on peut confirmer que notre instance AudioRecord est valide! Mettre à jour l'état interne
            audioRecord = recorder
            estEnregistrementActif = true

            //7 - Démarrer la boucle de lecture en arrière-plan
            startReadLoop()

            //8 - Informer les parties externes(UI) du changement de statut.
            notifyStateChanged(true)

            postcondition(audioRecord != null) { "audioRecord != null" }
            postcondition(estEnregistrementActif) { "estRecorder" }
            postcondition(recordingThread != null) { "recordingThread != null" }

            verifieInvariant()

        }catch (e: Exception){
            // Nettoyer en une seule fois en cas de panne
            cleanupAfterStartFailure()
            /*
            * Transformez les exceptions de bas niveau en exceptions métier unifiées,
            * en exposant un type d'exception unique au monde extérieur.
            * Les entités externes n'ont pas besoin de connaître tous
            * les détails des exceptions de bas niveau d'Android.
            * */
            throw when (e) {
                is RecorderException -> e
                is SecurityException ->
                    RecorderException("Accès au microphone refusé.", e)
                is IllegalStateException ->
                    RecorderException("État invalide lors du démarrage du recorder.", e)
                else ->
                    RecorderException("Échec du démarrage de la capture audio.", e)
            }
        }
    }

    /**
     * Arrête la capture audio en cours sans libérer définitivement les ressources.
     *
     * Cette méthode demande l'arrêt de la boucle de lecture, interrompt
     * l'enregistrement natif si nécessaire et attend brièvement la fin du thread
     * de lecture.
     *
     * Cette opération est idempotente : si aucune capture n'est active,
     * l'appel n'a pas d'effet.
     *
     * Après cet appel, le service peut être redémarré ultérieurement via
     * {@link #demarrer()}, tant que {@link #liberer()} n'a pas été invoquée.
     */
    fun arreter() {
        // 0 - Vérifier s'il y a eu un enregistrement en cours. ——idempotente
        if(!estEnregistrementActif){return}
        // 1 - Tout d'abord, changer le statut interner à faux.
        estEnregistrementActif = false
        // 2 - Récupérer la référence actuelle de l'audioRecord
        val recorder = audioRecord
        // 3 - Si le système enregistre effectivement, appeler la fonction stop().
        try{
            if(recorder?.recordingState== AudioRecord.RECORDSTATE_RECORDING){
                recorder.stop()
            }
        }catch(_: IllegalStateException){
            // Protection défensive : on évite de propager une exception de libération.
        }

        // 4 - Attendre de la fin du thread de lecture
        try{
            recordingThread?.join(500)
        }catch(_:InterruptedException){
            Thread.currentThread().interrupt()
        }finally {
            recordingThread=null
        }
    }

    /**
     * Libère les ressources natives associées à la capture audio.
     *
     * Cette méthode arrête d'abord toute capture en cours, puis libère
     * l'instance {@link AudioRecord} et remet l'état interne du service
     * à une configuration neutre.
     *
     * Elle doit être appelée lorsque le service n'est plus utilisé afin
     * d'éviter les fuites de ressources et de rendre le microphone à
     * l'environnement Android.
     *
     * Après la libération, aucune lecture audio ne doit être supposée active.
     */
    fun liberer() {
       arreter()

        try{
            audioRecord?.release()
        }catch (_:Exception){

        }finally {
            audioRecord = null
            recordingThread = null
            estEnregistrementActif = false
            bufferSizeInBytes = 0
        }

    }

    /**
     * Retourne {@code true} si une capture audio est actuellement active.
     *
     * @return {@code true} lorsque le service est en cours d'enregistrement,
     * sinon {@code false}.
     */
    fun isRecording(): Boolean {
        return estEnregistrementActif
    }

    /**
     * Enregistre le listener recevant les événements de capture audio.
     *
     * Passer {@code null} retire le listener courant.
     *
     * @param listener listener à notifier lors de la réception de blocs audio,
     * des erreurs ou des changements d'état.
     */
    fun setListener(listener: Listener?) {
        this.listener = listener
    }


    /**
     * Interface de callback utilisée pour recevoir les événements produits
     * par le service de capture audio.
     */
    interface Listener {
        /**
         * Appelé de manière asynchrone lorsqu'un nouveau bloc audio PCM est disponible.
         *
         * @param chunk bloc audio capturé, accompagné de ses métadonnées.
         */
        fun onAudioChunk(chunk: AudioChunk)

        /**
         * Appelé lorsqu'une erreur survient pendant l'initialisation
         * ou la capture audio.
         *
         * @param error exception métier décrivant l'échec rencontré.
         */
        fun onError(error: RecorderException)

        /**
         * Appelé lorsqu'un changement d'état d'enregistrement est observé.
         *
         * @param estEnregistrementActif {@code true} si la capture est active,
         * {@code false} sinon.
         */
        fun onRecordingStateChanged(estEnregistrementActif: Boolean)
    }


    //function prive

    /**
     * Calcule une taille de tampon valide pour {@link AudioRecord}.
     *
     * @return taille du tampon à utiliser pour la capture.
     * @throws RecorderException si Android ne fournit aucune taille exploitable.
     */
    private fun calculeBufferSizeInBytes():Int{
        val minBufferSize = AudioRecord.getMinBufferSize(
            AudioConfig.SAMPLE_RATE,
            AudioConfig.CHANNEL_CONFIG,
            AudioConfig.AUDIO_FORMAT
        )

        if(minBufferSize <= 0 ){
            throw RecorderException("Impossible de calculer une taille de tampon valide.")
        }
        return maxOf(minBufferSize, AudioConfig.CHUNK_SIZE_IN_BYTES)
    }

    /**
     * Crée une instance {@link AudioRecord} configurée selon {@link AudioConfig}.
     *
     * @param bufferSizeInBytes taille du tampon natif en octets.
     * @return instance d'enregistreur audio.
     */
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun creerAudioRecord(bufferSizeInBytes: Int):AudioRecord{
        precondition(bufferSizeInBytes > 0){"bufferSizeInBytes>0"}
        return  AudioRecord(
            AudioConfig.AUDIO_SOURCE,
            AudioConfig.SAMPLE_RATE,
            AudioConfig.CHANNEL_CONFIG,
            AudioConfig.AUDIO_FORMAT,
            bufferSizeInBytes
        )
    }

    private fun notifyStateChanged(estRecorder:Boolean){
        listener?.onRecordingStateChanged(estRecorder)
    }

    /**
     * Lance le thread dédié à la lecture continue des données audio.
     */
    private fun startReadLoop(){
        precondition(audioRecord != null) { "audioRecord != null" }
        precondition(recordingThread == null) { "recordingThread == null" }
        recordingThread = Thread {
            readAudioLoop()
        }.apply {
            name = "AudioRecorderThread"
            start()
        }
    }

    /**
     * Exécute la boucle de lecture continue des blocs audio.
     *
     * Tant que la capture est active, cette méthode lit les données PCM depuis
     * {@link AudioRecord}, construit des {@link AudioChunk} et les transmet
     * au listener.
     *
     * En cas d'erreur de lecture, une {@link RecorderException} est notifiée
     * au listener avant la fermeture logique de la boucle.
     */
    private fun readAudioLoop(){
        val recorder = audioRecord ?: return
        val buffer = ByteArray(AudioConfig.CHUNK_SIZE_IN_BYTES)

        try {
            while (estEnregistrementActif) {
                val bytesRead = recorder.read(buffer, 0, buffer.size)

                when {
                    bytesRead > 0 -> {
                        val chunk = buildAudioChunk(buffer, bytesRead)
                        listener?.onAudioChunk(chunk)
                    }
                    bytesRead < 0 -> {
                        throw RecorderException("Erreur de lecture audio: code=$bytesRead")
                    }
                }
            }
        } catch (e: RecorderException) {
            listener?.onError(e)
        } finally {
            try {
                if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    recorder.stop()
                }
            } catch (_: IllegalStateException) {
            }
            estEnregistrementActif = false
            recordingThread = null
            notifyStateChanged(false)
        }
    }

    /**
     * Construit un {@link AudioChunk} à partir d'un tampon PCM lu.
     *
     * @param buffer tampon source contenant les données audio brutes.
     * @param bytesRead nombre d'octets effectivement lus.
     * @return bloc audio immutable prêt à être transmis au listener.
     */
    private fun buildAudioChunk(buffer:ByteArray,bytesRead:Int):AudioChunk{
        precondition( buffer.isNotEmpty() && bytesRead>0){"buffer.size>0 && bytesRead>0"}
        val pcmCopy = buffer.copyOf(bytesRead)
        val samples = bytesRead / AudioConfig.BYTES_PER_SAMPLE
        val peak = computePeak16Bit(pcmCopy)

        return AudioChunk(
            sampleRate = AudioConfig.SAMPLE_RATE,
            bytesRead = bytesRead,
            samples = samples,
            pcmBytes = pcmCopy,
            peak = peak,
            timestampMs = System.currentTimeMillis()
        )
    }

    /**
     * Calcule l'amplitude maximale absolue d'un bloc PCM 16 bits little-endian.
     *
     * @param pcmBytes données audio brutes.
     * @return valeur de crête du signal sur le bloc fourni.
     */
    private fun computePeak16Bit(pcmBytes: ByteArray):Int{
        if (pcmBytes.size<2){
            return 0
        }

        var peak =0
        var index =0
        while (index+1 < pcmBytes.size){
            val low = pcmBytes[index].toInt() and 0xFF
            val high = pcmBytes[index+1].toInt()
            val sample =(high shl 8) or low
            val amplitude = if (sample == Short.MIN_VALUE.toInt()){
                32768
            }else{
                kotlin.math.abs(sample)
            }
            if (amplitude > peak){
                peak = amplitude
            }
            index +=2
        }
        return peak
    }

    private fun cleanupAfterStartFailure() {
        try {
            audioRecord?.release()
        } catch (_: Exception) {
        } finally {
            audioRecord = null
            estEnregistrementActif = false
            recordingThread = null
            bufferSizeInBytes = 0
        }
    }

    private fun avoirRecorderAudioPermission():Boolean{
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        )==PackageManager.PERMISSION_GRANTED
    }

    private fun verifieInvariant(){
        //La taille du tampon ne peut pas être négative, sinon throw une exception
        invariant(bufferSizeInBytes>=0){ "bufferSizeInBytes >= 0" }
        //Le statut « Enregistrement en cours, mais instance audioRecord est introuvable » n'est pas autorisé.
        invariant(!(estEnregistrementActif && audioRecord == null)){"!(estEnregistrementActif && audioRecord == null)"}
        //L'état « le thread de lecture existe, mais instance audioRecord est introuvable » n'est pas autorisé.
        invariant(!(recordingThread != null && audioRecord == null)){"!(recordingThread != null && audioRecord == null)"}

    }

}