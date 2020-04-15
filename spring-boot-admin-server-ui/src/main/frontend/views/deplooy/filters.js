export const filters = {
  operationFilter (key) {
    const keyMap = {
      0: '',
      1: '部署',
      2: '上线',
      3: '下线',
      4: '回滚',
      5: ''
    }
    return keyMap[key]
  },
  statusFilter (mapKey) {
    var hasUp = false
    var hasDown = false
    var hasUnknown = false
    for (var i = 0; i < mapKey.length; ++i) {
      const instance = mapKey[i]
      if (instance.statusInfo.status === 'UP') {
        hasUp = true
      }
      if (instance.statusInfo.status === 'UNKNOWN') {
        hasUnknown = true
      }
      if (instance.statusInfo.status === 'DOWN' || instance.statusInfo.status === 'OFFLINE') {
        hasDown = true
      }
    }
    if (hasUp && (hasDown || hasUnknown)) {
      return 'PARTUP'
    }
    if (hasDown && hasUnknown) {
      return 'DOWNANDUNKNOWN'
    }
    if (hasUp) {
      return 'UP'
    }
    if (hasDown && !hasUnknown) {
      return 'DOWN'
    }
  },
  finalStatus (key) {
    const keyMap = {
      'UP': '运行',
      'DOWN': '停止',
      'UNKNOWN': '未知',
      'PARTUP': '部分运行',
      'DOWNANDUNKNOWN': '停止+未知'
    }
    return keyMap[key]
  }
}
