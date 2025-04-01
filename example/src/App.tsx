import * as React from 'react';

import { StyleSheet, View, Text, Button, TextInput, Platform } from 'react-native';
import { AsrSdk } from 'react-native-tencent-cloud-asr-sdk';

declare const global: {
  HermesInternal: null | {};
  __DEV__: boolean;
  __TEMPORARY_CACHE_PATH__: string;
};

export default function App() {
  const [result, setResult] = React.useState<string>('');

  const resultMap = React.useRef<{ [key: string]: string }>({});

  const refreshResult = React.useCallback(() => {
    const resultLists: string[] = [];
    for (const key in resultMap.current) {
      if (Object.prototype.hasOwnProperty.call(resultMap.current, key)) {
        const element: string = resultMap.current[key] ?? '';
        resultLists.push(element);
      }
    }
    const text = resultLists.join('\r\n');
    console.log(`最终的文字`, text);
    setResult(text);
  }, []);

  const listener: AsrSdk.RecorderListener = React.useMemo(() => {
    return {
      onRecognizeResult_onSliceSuccess: (params: {
        result: AsrSdk.AudioRecognizeResult;
        seq: number;
      }) => {
        const text = params.result.text;
        const seq = params.seq;
        console.log(
          `这是页面中的监听器，onRecognizeResult_onSliceSuccess`,
          text
        );
        resultMap.current[`${seq}`] = text;
        refreshResult();
      },

      onRecognizeResult_onSegmentSuccess: (params: {
        result: AsrSdk.AudioRecognizeResult;
        seq: number;
      }) => {
        const text = params.result.text;
        const seq = params.seq;
        console.log(
          `这是页面中的监听器，onRecognizeResult_onSegmentSuccess`,
          text
        );
        resultMap.current[`${seq}`] = text;
        refreshResult();
      },

      onRecognizeResult_onSuccess: (_params: { result: string }) => {
        console.log(`这是页面中的监听器，onRecognizeResult_onSuccess`);
      },

      onRecognizeResult_onFailure: (params: {
        code: string;
        message: string;
      }) => {
        console.log(`这是页面中的监听器，onRecognizeResult_onFailure`, params);
      },

      onRecognizeState_onStartRecord: (_params: {}) => {
        console.log(`这是页面中的监听器，onRecognizeState_onStartRecord`);
      },

      onRecognizeState_onStopRecord: (_params: {
        filePath: string;
        fileName: string;
      }) => {
        console.log(`这是页面中的监听器，onRecognizeState_onStopRecord`);
      },

      onRecognizeState_onVoiceVolume: (_params: { volume: number }) => {
        // console.log(`这是页面中的监听器，onRecognizeState_onVoiceVolume`);
      },

      onRecognizeState_onVoiceDb: (_params: { volumeDb: string }) => {
        // console.log(`这是页面中的监听器，onRecognizeState_onVoiceDb`);
      },
    };
  }, [refreshResult]);

  React.useEffect(() => {
    AsrSdk.addListener(listener);
    return () => {
      AsrSdk.removeListener();
    };
  }, [listener]);

  const [volumeValue, setVolumeValue] = React.useState('3');

  // 获取缓存目录路径
  const getCachePath = () => {
    return './chat/wav';
  };

  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>
      <Button
        title="init初始化"
        onPress={() => {
          AsrSdk.init(
            1304799512,
            0,
            '',
            '',
            "",
          );
        }}
      />
      <Button
        title="开始录音"
        onPress={() => {
          AsrSdk.startRecorder();
        }}
      />
      <Button
        title="结束录音"
        onPress={() => {
          AsrSdk.stopRecorder();
        }}
      />
      <TextInput
        value={volumeValue}
        onChangeText={(text) => setVolumeValue(text)}
      />
      <Button
        title="设置音量大小"
        onPress={() => {
          // startRecorder();
          AsrSdk.cancelRecorder();
        }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
