/**
 * @type {import('@react-native-community/cli-types').UserDependencyConfig}
 */
module.exports = {
  dependency: {
    platforms: {
      android: {
        packageInstance: 'new TencentCloudAsrSdkPackage()',
        cmakeListsPath: 'generated/jni/CMakeLists.txt',
      },
    },
  },
};
