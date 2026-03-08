import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';

export default function MeasureScreen() {
  return (
    <View style={styles.container}>
      <Text style={styles.title}>TapTone Luthier</Text>
      <Text style={styles.subtitle}>Tap Tone Frequency Analyzer</Text>

      <View style={styles.card}>
        <Text style={styles.label}>Status</Text>
        <Text style={styles.value}>Idle</Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.label}>Dominant Frequency</Text>
        <Text style={styles.value}>-- Hz</Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.label}>Confidence</Text>
        <Text style={styles.value}>-- %</Text>
      </View>

      <View style={styles.buttonRow}>
        <TouchableOpacity style={styles.primaryButton}>
          <Text style={styles.buttonText}>Start Measure</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.secondaryButton}>
          <Text style={styles.buttonText}>Stop</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.resultBox}>
        <Text style={styles.label}>Result</Text>
        <Text style={styles.resultText}>No measurement yet.</Text>
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