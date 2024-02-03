import * as React from 'react';
import RNFS from 'react-native-fs';
import { StyleSheet, View, Text, Button, TextInput, ToastAndroid } from 'react-native';
import { AsrSdk } from 'react-native-tencent-asr-sdk';

/**
 * 腾讯云ASR RN SDK 示例
 * @returns
 */
export default function App() {
  // 录音后的本地文件，默认 WAV 格式
  const [localFile,setLocalFile] = React.useState<string>("");

  // 最终的识别结果
  const [result, setResult] = React.useState<string>("");
  // 正在识别中的 分片结果集合
  const resultMap = React.useRef<{[key:string]:string}>({});

  const [errorMsg,setErrorMsg] = React.useState<string>();

  /** 将 分片识别的结果集合 组装整合 成一段话 */
  const refreshResult = React.useCallback(()=>{
      const resultLists :string[]=[];
      for (const key in resultMap.current) {
        if (Object.prototype.hasOwnProperty.call(resultMap.current, key)) {
          const element:string = resultMap.current[key]??"";
          resultLists.push(element);
        }
      }
      const text = resultLists.join("");
      console.log(`最终的文字`,text);
      setResult(text);
  },[]);

  const listener: AsrSdk.RecorderListener  = React.useMemo(()=>{
    return {
      onRecognizeResult_onSliceSuccess:(params:{result:AsrSdk.AudioRecognizeResult,seq:number})=>{
        const text = params.result.text;
        const seq = params.seq;
        console.log(`这是页面中的监听器，onRecognizeResult_onSliceSuccess`,text);
        resultMap.current[`${seq}`] =text;
        refreshResult();
      },

      onRecognizeResult_onSegmentSuccess:(params:{result:AsrSdk.AudioRecognizeResult,seq:number})=>{
        const text = params.result.text;
        const seq = params.seq;
        console.log(`这是页面中的监听器，onRecognizeResult_onSegmentSuccess`,text);
        resultMap.current[`${seq}`] =text;
        refreshResult();
      },

      onRecognizeResult_onSuccess:(params:{result:string})=>{
        console.log(`这是页面中的监听器，onRecognizeResult_onSuccess`);
      },

      onRecognizeResult_onFailure:(params:{code:string,message:string})=>{
        console.log(`这是页面中的监听器，onRecognizeResult_onFailure`,params);
      },

      onRecognizeState_onStartRecord :(params:{})=>{
        console.log(`这是页面中的监听器，onRecognizeState_onStartRecord`);
      },

      onRecognizeState_onStopRecord:(params:{filePath:string,fileName:string})=>{
        const {filePath,fileName} = params;
        const tempLocalFile = filePath+"/"+fileName;
        console.log(`这是页面中的监听器，onRecognizeState_onStopRecord,本次录音的本地文件路径为：`,tempLocalFile);
        setLocalFile(tempLocalFile)
      },

      onRecognizeState_onVoiceVolume:(params:{volume:number})=>{
        // console.log(`这是页面中的监听器，onRecognizeState_onVoiceVolume`);
      },

      onRecognizeState_onVoiceDb :(params:{volumeDb:string})=>{
        // console.log(`这是页面中的监听器，onRecognizeState_onVoiceDb`);
      },
    }
  },[]);

  React.useEffect(() => {
    AsrSdk.addListener(listener);
    return ()=>{
      AsrSdk.removeListener();
    }
  }, []);


  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>
      <Button title='init初始化' onPress={()=>{
        const savePath = RNFS.CachesDirectoryPath + '/chat/wav';
        if (!RNFS.exists(savePath)) {
          RNFS.mkdir(savePath);
        }
        // 下面的换成你自己的 Key信息
        const appId= 0;
        const projectId = 0;
        const secretId = ""
        const secretKey= "";
        AsrSdk.init(appId,projectId,secretId,secretKey,savePath);
      }}/>
      <Button title='开始录音并实时转换为文字' onPress={()=>{
        AsrSdk.startRecorder();
      }}/>
      <Button title='结束录音并结束转换为文字' onPress={()=>{
        AsrSdk.stopRecorder();
      }}/>
      <Button title='取消录音并放弃转换内容' onPress={()=>{
        // startRecorder();
        AsrSdk.cancelRecorder();
      }}/>
      <Button title='识别现有的音频文件转文字' onPress={()=>{
        if(localFile){
          AsrSdk.recognizerFile(localFile,"wav").then(result=>{
            setResult(result);
            console.log(`本地文件识别结束`)
          });
        }else{
          setErrorMsg("请先录音，录音结束后再播放");
        }
      }}/>
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
