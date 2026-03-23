import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Alert,
  PermissionsAndroid,
  Platform,
} from 'react-native';

import {
  TapToneRecorder,
  AudioChunkEvent,
} from '../services/TapToneRecorder';

/**
 * Écran principal de mesure des tap tones.
 *
 * Responsabilités :
 * - démarrer et arrêter une session d’enregistrement ;
 * - écouter les événements natifs envoyés par le module Android ;
 * - afficher l’état courant de la capture ;
 * - présenter les résultats bruts de mesure en attendant
 *   l’intégration complète de l’analyse FFT.
 *
 * Cette première version sert surtout à valider la chaîne complète :
 * UI React Native -> module natif -> AudioRecord -> événements JS.
 */
export default function MeasureScreen() {
  const [status, setStatus] = useState('Idle');
  const [dominantFrequency, setDominantFrequency] = useState('--');
  const [confidence, setConfidence] = useState('--');
  const [resultText, setResultText] = useState('No measurement yet.');
  const [isRecording, setIsRecording] = useState(false);

  useEffect(() => {
    /**
         * Écoute les blocs audio envoyés par la couche native.
         *
         * Pour l’instant, cet écran ne fait pas encore l’analyse fréquentielle
         * complète. On affiche seulement des informations de validation
         * (peak, nombre d’échantillons, sample rate) pour confirmer que
         * les données PCM arrivent correctement depuis Android.
         */
    const chunkSub = TapToneRecorder.addAudioChunkListener(
      (event: AudioChunkEvent) => {
        setResultText(
          `Chunk received | peak=${event.peak} | samples=${event.samples} | sampleRate=${event.sampleRate}`
        );

        /**
         * Valeur temporaire de confiance utilisée comme placeholder UI.
         * Cette estimation est basée uniquement sur le peak du chunk
         * et ne représente pas encore une vraie mesure acoustique.
         * Elle sera remplacée plus tard par une confiance dérivée
         * de l’analyse FFT / énergie spectrale.
         */
        const fakeConfidence = Math.min(100, Math.round((event.peak / 32767) * 100));
        setConfidence(`${fakeConfidence}`);

        /**
        * La fréquence dominante n’est pas encore calculée dans cette étape.
        * Elle sera renseignée lorsque la chaîne FFT côté JavaScript
        * sera intégrée.
        */
        setDominantFrequency('--');
      }
    );

    /**
    * Écoute les erreurs remontées par le recorder natif.
    *
    * Toute erreur de capture est affichée dans l’UI et dans une boîte
    * de dialogue afin de rendre le diagnostic plus visible pendant
    * la phase de développement.
    */
    const errorSub = TapToneRecorder.addRecorderErrorListener(event => {
      setStatus('Error');
      setResultText(event.message);
      Alert.alert('Recorder Error', event.message);
    });

    /**
         * Synchronise l’état d’enregistrement avec la couche native.
         *
         * Cette écoute permet de garder l’interface cohérente même si
         * l’état change depuis le module natif.
         */
    const stateSub = TapToneRecorder.addRecorderStateChangedListener(event => {
      setIsRecording(event.isRecording);
      setStatus(event.isRecording ? 'Recording' : 'Idle');
    });

    /**
     * Lit l’état initial du recorder au montage de l’écran.
     *
     * Cela évite d’afficher un état local incohérent si le module natif
     * est déjà actif ou s’il a conservé un état après un rechargement.
     */
    const syncInitialState = async () => {
      try {
        const recording = await TapToneRecorder.isRecording();
        setIsRecording(recording);
        setStatus(recording ? 'Recording' : 'Idle');
      } catch (error) {
        setStatus('Unavailable');
      }
    };

    syncInitialState();

     /**
     * Nettoyage des abonnements lors du démontage de l’écran
     * pour éviter les fuites mémoire et les callbacks orphelins.
     */
    return () => {
      chunkSub.remove();
      errorSub.remove();
      stateSub.remove();
    };
  }, []);

  /**
   * Démarre une session de mesure.
   *
   * Étapes :
   * 1. demander la permission microphone ;
   * 2. démarrer le recorder natif ;
   * 3. mettre à jour le texte de résultat pour informer l’utilisateur.
   */
  const handleStart = async () => {
     try {
        const granted = await requestMicrophonePermission();
        if (!granted) {
          Alert.alert('Permission required', 'Microphone permission was denied.');
          return;
        }

        await TapToneRecorder.startRecording();
        setResultText('Recording started. Waiting for audio chunks...');
      } catch (error) {
        const message =
          error instanceof Error ? error.message : 'Failed to start recording';
        setStatus('Error');
        setResultText(message);
        Alert.alert('Start Error', message);
      }
  };

  /**
     * Arrête la session de mesure en cours.
     */
  const handleStop = async () => {
    try {
      await TapToneRecorder.stopRecording();
      setResultText('Recording stopped.');
    } catch (error) {
      const message =
        error instanceof Error ? error.message : 'Failed to stop recording';
      setStatus('Error');
      setResultText(message);
      Alert.alert('Stop Error', message);
    }
  };

/**
   * Demande la permission microphone sur Android.
   *
   * Sur les autres plateformes, on retourne directement `true`
   * dans cette version du MVP, car le flux principal actuel
   * cible Android.
   */
  async function requestMicrophonePermission(): Promise<boolean> {
    if (Platform.OS !== 'android') {
      return true;
    }

    try {
      const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.RECORD_AUDIO,
        {
          title: 'Microphone Permission',
          message: 'TapTone Luthier needs microphone access to record tap tones.',
          buttonPositive: 'OK',
        }
      );

      return granted === PermissionsAndroid.RESULTS.GRANTED;
    } catch {
      return false;
    }
  }



  return (
    <View style={styles.container}>
      <Text style={styles.title}>TapTone Luthier</Text>
      <Text style={styles.subtitle}>Tap Tone Frequency Analyzer</Text>

      <View style={styles.card}>
        <Text style={styles.label}>Status</Text>
        <Text style={styles.value}>{status}</Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.label}>Dominant Frequency</Text>
        <Text style={styles.value}>
          {dominantFrequency === '--' ? '-- Hz' : `${dominantFrequency} Hz`}
        </Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.label}>Confidence</Text>
        <Text style={styles.value}>
          {confidence === '--' ? '-- %' : `${confidence} %`}
        </Text>
      </View>

      <View style={styles.buttonRow}>
        <TouchableOpacity
          style={[
            styles.primaryButton,
            isRecording && styles.disabledButton,
          ]}
          onPress={handleStart}
          disabled={isRecording}
        >
          <Text style={styles.buttonText}>Start Measure</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[
            styles.secondaryButton,
            !isRecording && styles.disabledButton,
          ]}
          onPress={handleStop}
          disabled={!isRecording}
        >
          <Text style={styles.buttonText}>Stop</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.resultBox}>
        <Text style={styles.label}>Result</Text>
        <Text style={styles.resultText}>{resultText}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f7fa',
    padding: 20,
    paddingTop: 60,
  },
  title: {
    fontSize: 28,
    fontWeight: '700',
    marginBottom: 6,
  },
  subtitle: {
    fontSize: 16,
    color: '#666',
    marginBottom: 24,
  },
  card: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 12,
    marginBottom: 12,
  },
  label: {
    fontSize: 14,
    color: '#666',
    marginBottom: 6,
  },
  value: {
    fontSize: 22,
    fontWeight: '600',
    color: '#111',
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 12,
    marginBottom: 20,
    gap: 12,
  },
  primaryButton: {
    flex: 1,
    backgroundColor: '#1f6feb',
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: 'center',
  },
  secondaryButton: {
    flex: 1,
    backgroundColor: '#6c757d',
    paddingVertical: 14,
    borderRadius: 10,
    alignItems: 'center',
  },
  disabledButton: {
    opacity: 0.5,
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
  resultBox: {
    backgroundColor: '#fff',
    padding: 16,
    borderRadius: 12,
  },
  resultText: {
    fontSize: 16,
    color: '#333',
  },
});