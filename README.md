# react-native-tencent-asr-sdk

腾讯云ASR语音识别SDK-RN版本

## Installation

```sh
npm install react-native-tencent-asr-sdk
```

or

```sh
yarn add react-native-tencent-asr-sdk
```

## Usage

```js
import { Asr } from 'react-native-tencent-asr-sdk';

/** 1. 初始化SDK */
const init = ()=>{
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
}

// 2. 添加监听器
const listener: AsrSdk.RecorderListener  = React.useMemo(()=>{
  return {
    onRecognizeResult_onSliceSuccess:(params:{result:AsrSdk.AudioRecognizeResult,seq:number})=>{
    },
    ...
    onRecognizeState_onVoiceDb :(params:{volumeDb:string})=>{
      // console.log(`这是页面中的监听器，onRecognizeState_onVoiceDb`);
    },
  }
},[]);

React.useEffect(() => {
  // 在生命周期函数中添加/销毁监听器
  AsrSdk.addListener(listener);
  return ()=>{
    AsrSdk.removeListener();
  }
}, []);

// 3. 你的业务代码，根据你的业务场景选择调用SDK函数，结合监听器使用
// 开始录音 并 实时识别成文字
AsrSdk.startRecorder();
// 结束录音 并 停止识别成文字
AsrSdk.stopRecorder();
// 结束录音 并 放弃识别结果
AsrSdk.cancelRecorder();
// 开始识别本地文件
AsrSdk.recognizerFile
```

## 注意事项

1. 目前实时语音转文字，是直接把腾讯官网的Demo做了个RN封装。意味着开始录音时就在调用SDK，在费用上有很大的浪费。如果

## 后续展望:

1. 录音和识别分离，

* 录音只是确保最基本的能生成一个音频文件
* 需要识别时才去调用腾讯API，减少费用

2. 网络依赖
   后续考虑集成离线版语音识别SDK

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

---

Made with [create-react-native-library](https://github.com/callstack/react-native-builder-bob)
