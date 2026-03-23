/**
 * Pont React Native vers le module natif Android TapToneRecorder.
 *
 * Ce fichier centralise l’accès aux fonctions de contrôle de l’enregistreur
 * ainsi qu’aux événements émis par la couche native pendant la capture audio.
 */

import {
  NativeModules,
  NativeEventEmitter,
  EmitterSubscription,
  Platform,
} from 'react-native';


/**
 * Représente la charge utile envoyée par la couche native
 * lorsqu’un nouveau bloc audio PCM est disponible.
 *
 * Chaque événement correspond à un segment capturé par l’enregistreur
 * natif, encodé en Base64 afin de pouvoir transiter par le bridge
 * React Native.
 */
export type AudioChunkEvent = {
  sampleRate: number;  /** Fréquence d’échantillonnage du signal audio, en Hz. */
  bytesRead: number;   /** Nombre réel d’octets lus pour ce bloc audio. */
  samples: number;     /** Nombre d’échantillons PCM contenus dans ce bloc. */
  peak: number;        /** Amplitude maximale détectée dans ce bloc audio. */
  timestampMs: number; /** Horodatage associé au bloc, en millisecondes. */
  pcmBase64: string;   /** Données PCM brutes encodées en Base64. */
};

/**
 * Représente la charge utile envoyée lorsqu’une erreur
 * survient dans la couche native d’enregistrement.
 */
export type RecorderErrorEvent = {
  message: string;
};

/**
 * Représente la charge utile envoyée lorsqu’un changement
 * d’état de l’enregistreur se produit.
 */
export type RecorderStateChangedEvent = {
  isRecording: boolean;
};

/**
 * Définit le contrat TypeScript du module natif TapToneRecorder
 * exposé via React Native.
 *
 * Cette interface décrit les méthodes accessibles depuis la couche
 * TypeScript pour piloter l’enregistrement audio natif.
 */
type TapToneRecorderNativeModule = {
  startRecording: () => Promise<void>;
  stopRecording: () => Promise<void>;
  isRecording: () => Promise<boolean>;
};

/**
 * Référence vers le module natif TapToneRecorder enregistré
 * dans le runtime React Native.
 *
 * Cette valeur peut être indéfinie si le module n’a pas été
 * correctement enregistré ou si l’application n’a pas été recompilée.
 */
const tapToneRecorderNM = NativeModules.TapToneRecorder as
  | TapToneRecorderNativeModule
  | undefined;
/**
 * Signale en phase de développement qu’un module natif Android attendu
 * n’a pas été trouvé.
 *
 * Cela indique généralement un problème d’enregistrement du module
 * natif ou la nécessité de reconstruire l’application.
 */
if (Platform.OS === 'android' && !tapToneRecorderNM) {
  console.warn(
    'TapToneRecorder native module not found. Check package registration and rebuild the app.'
  );
}

/**
 * Émetteur d’événements permettant de recevoir les événements
 * asynchrones envoyés par la couche native.
 *
 * Il n’est initialisé que si le module natif est disponible.
 */
const eventEmitter = tapToneRecorderNM
  ? new NativeEventEmitter(NativeModules.TapToneRecorder)
  : null;

/**
 * Façade TypeScript autour du module natif TapToneRecorder.
 *
 * Cet objet fournit une API frontale typée pour :
 * - démarrer l’enregistrement ;
 * - arrêter l’enregistrement ;
 * - interroger l’état courant ;
 * - s’abonner aux événements natifs émis pendant la capture.
 *
 * Il constitue le point d’entrée côté React Native pour interagir
 * avec la couche d’enregistrement Android.
 */
export const TapToneRecorder = {
  /**
   * Démarre l’enregistrement audio natif.
   *
   * @returns Une promesse résolue lorsque l’enregistrement a été démarré.
   * @throws {Error} Si le module natif n’est pas disponible.
   */
  async startRecording(): Promise<void> {
    if (!tapToneRecorderNM) {
      throw new Error('TapToneRecorder native module not available');
    }
    return tapToneRecorderNM.startRecording();
  },

 /**
   * Arrête l’enregistrement audio natif.
   *
   * @returns Une promesse résolue lorsque l’enregistrement a été arrêté.
   * @throws {Error} Si le module natif n’est pas disponible.
   */
  async stopRecording(): Promise<void> {
    if (!tapToneRecorderNM) {
      throw new Error('TapToneRecorder native module not available');
    }
    return tapToneRecorderNM.stopRecording();
  },

  /**
   * Indique si l’enregistreur natif est actuellement actif.
   *
   * @returns Une promesse contenant l’état courant de l’enregistreur.
   * @throws {Error} Si le module natif n’est pas disponible.
   */
  async isRecording(): Promise<boolean> {
    if (!tapToneRecorderNM) {
      throw new Error('TapToneRecorder native module not available');
    }
    return tapToneRecorderNM.isRecording();
  },

  /**
   * Abonne un écouteur aux événements de blocs audio envoyés
   * par la couche native.
   *
   * @param listener Fonction appelée à chaque réception d’un bloc audio.
   * @returns L’abonnement à conserver pour pouvoir le retirer plus tard.
   * @throws {Error} Si l’émetteur d’événements natif n’est pas disponible.
   */
  addAudioChunkListener(
    listener: (event: AudioChunkEvent) => void
  ): EmitterSubscription {
    if (!eventEmitter) {
      throw new Error('TapToneRecorder event emitter not available');
    }
    return eventEmitter.addListener('audioChunk', listener);
  },

  /**
   * Abonne un écouteur aux événements d’erreur envoyés
   * par la couche native.
   *
   * @param listener Fonction appelée lorsqu’une erreur d’enregistrement survient.
   * @returns L’abonnement à conserver pour pouvoir le retirer plus tard.
   * @throws {Error} Si l’émetteur d’événements natif n’est pas disponible.
   */
  addRecorderErrorListener(
    listener: (event: RecorderErrorEvent) => void
  ): EmitterSubscription {
    if (!eventEmitter) {
      throw new Error('TapToneRecorder event emitter not available');
    }
    return eventEmitter.addListener('recorderError', listener);
  },

  /**
   * Abonne un écouteur aux changements d’état de l’enregistreur natif.
   *
   * @param listener Fonction appelée lorsque l’état d’enregistrement change.
   * @returns L’abonnement à conserver pour pouvoir le retirer plus tard.
   * @throws {Error} Si l’émetteur d’événements natif n’est pas disponible.
   */
  addRecorderStateChangedListener(
    listener: (event: RecorderStateChangedEvent) => void
  ): EmitterSubscription {
    if (!eventEmitter) {
      throw new Error('TapToneRecorder event emitter not available');
    }
    return eventEmitter.addListener('recorderStateChanged', listener);
  },
};