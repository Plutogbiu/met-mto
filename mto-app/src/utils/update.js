import { fetchLatestVersion } from '../api/version'

let updateChecking = false

function getRuntimeVersion() {
  return new Promise((resolve) => {
    // #ifndef APP-PLUS
    resolve({ versionName: '', versionCode: 0 })
    // #endif

    // #ifdef APP-PLUS
    if (typeof plus === 'undefined' || !plus.runtime) {
      resolve({ versionName: '', versionCode: 0 })
      return
    }
    plus.runtime.getProperty(plus.runtime.appid, (info) => {
      resolve({
        versionName: info.version || plus.runtime.version || '',
        versionCode: Number(info.versionCode || plus.runtime.versionCode || 0),
      })
    })
    // #endif
  })
}

function openDownloadUrl(downloadUrl) {
  // #ifdef APP-PLUS
  plus.runtime.openURL(downloadUrl)
  // #endif
}

function installWgt(downloadUrl) {
  // #ifdef APP-PLUS
  uni.showLoading({ title: '正在下载更新' })
  uni.downloadFile({
    url: downloadUrl,
    success: (response) => {
      if (response.statusCode !== 200 || !response.tempFilePath) {
        uni.hideLoading()
        uni.showToast({ title: '更新包下载失败', icon: 'none' })
        return
      }
      plus.runtime.install(
        response.tempFilePath,
        { force: true },
        () => {
          uni.hideLoading()
          uni.showModal({
            title: '更新完成',
            content: '应用将重启以完成更新',
            showCancel: false,
            success: () => {
              plus.runtime.restart()
            },
          })
        },
        () => {
          uni.hideLoading()
          uni.showToast({ title: '更新安装失败', icon: 'none' })
        }
      )
    },
    fail: () => {
      uni.hideLoading()
      uni.showToast({ title: '更新包下载失败', icon: 'none' })
    },
  })
  // #endif
}

function applyUpdate(versionInfo) {
  if (!versionInfo?.downloadUrl) {
    return
  }
  const updateType = String(versionInfo.updateType || '').toLowerCase()
  if (updateType === 'wgt') {
    installWgt(versionInfo.downloadUrl)
    return
  }
  openDownloadUrl(versionInfo.downloadUrl)
}

function showUpdateDialog(versionInfo) {
  const versionName = versionInfo.versionName || ''
  const releaseNotes = versionInfo.releaseNotes || '发现新版本，请更新后继续使用'
  uni.showModal({
    title: versionName ? `发现新版本 ${versionName}` : '发现新版本',
    content: releaseNotes,
    showCancel: !versionInfo.forceUpdate,
    cancelText: '稍后',
    confirmText: '立即更新',
    success: (result) => {
      if (result.confirm) {
        applyUpdate(versionInfo)
      }
    },
  })
}

export async function checkAppUpdate() {
  // #ifndef APP-PLUS
  return
  // #endif

  if (updateChecking) {
    return
  }
  updateChecking = true
  try {
    const current = await getRuntimeVersion()
    const versionInfo = await fetchLatestVersion({
      platform: 'android',
      currentVersionCode: current.versionCode,
    })
    if (versionInfo?.needUpdate) {
      showUpdateDialog(versionInfo)
    }
  } catch (error) {
    console.warn('App 更新检查失败', error)
  } finally {
    updateChecking = false
  }
}
