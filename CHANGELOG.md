# DGmodules 更新日志

## 2026-07-20

### 维护：GitHub Actions Node.js 24 兼容

- 更新 `.github/workflows/build.yml`：将 `actions/checkout` 升级至 `v6`、`actions/setup-java` 升级至 `v5`、`gradle/actions/setup-gradle` 升级至 `v6`，使 GitHub 构建使用 Node.js 24 运行时，不再触发 Node.js 20 弃用警告；JDK 版本仍保持为 21，Gradle 构建命令不变。

### 修复：JEI 可选依赖的 GitHub 构建

- 修改 `build.gradle`，使用 `mezz.jei:jei-1.21.1-neoforge-api:19.27.0.340` 的 `compileOnly` 依赖和 BlameJared Maven 仓库，移除对 `run/mods` 本地 JEI JAR 的依赖；GitHub 干净环境可以编译 JEI 插件，发布物仍不会携带 JEI。
- 修改 `META-INF/neoforge.mods.toml`，将 JEI 注册为客户端可选依赖；未安装 JEI 时 DGmodules 仍可加载，只是不注册自定义 JEI 配方分类。

### 调整：护主模块合成配方

- 修改 `data/dgmodules/recipe/modules/host_integrity_module.json`，移除 `dgmodules:shield_control_booster_module` 消耗材料；护主模块不再要求额外消耗护盾控制增幅模块。

### 重做：稳定模块 UUID 监测与所有者归还

- 修改 `HostIntegrityModuleEntity` 的 `CODEC`、`STREAM_CODEC` 和复制逻辑，在原稳定 UUID 外持久化绑定玩家 UUID；旧宿主首次被服务端玩家扫描时自动绑定，复制出的宿主不会因 UUID 冲突而重新编号。
- 新增 `HostIntegrityLedger`，以世界 SavedData 注册名 `dgmodules_host_integrity` 持久化稳定 UUID、绑定玩家、权威位置、首次发现序号和离线待归还物品，服务器重启后仍能判断原宿主及后出现的副本。
- 新增 `HostIntegrityMonitor`，通过玩家操作、登录、装备与 Curios 变化、物品实体加载和拾取事件即时检查，并按玩家错峰进行周期兜底扫描；范围包含背包、快捷栏、护甲、副手、光标物品、Curios 普通栏位和外观栏位。
- 同一玩家出现多个相同稳定 UUID 宿主时保留账本记录的权威位置并清除后出现的宿主；其他玩家无法拾取或持有已绑定宿主，错误获得的宿主会从其栏位移除并尝试归还所有者。
- 所有者在线且背包有空位时直接归还；背包已满时在玩家视线前方生成无重力、发光、无限寿命且仅允许所有者拾取的浮空宿主；所有者离线时写入待归还队列并在下次登录处理。
- 修改 `mixins/dgmodules.json`，继续启用原有物品栏、Curios、ItemEntity、丢弃和组件写入拦截，阻止非玩家本人直接更改、移除或丢弃受保护宿主；服务端 UUID 监测作为第二层校验，清除后出现的同 UUID 宿主直到只剩一个权威实例。
- 新增 `HostIntegrityTooltip` 及语言键 `tooltip.dgmodules.host_integrity.uuid`，受保护宿主 tooltip 显示完整稳定 UUID，便于与服务端清理、拒绝拾取和归还日志核对。

## 2026-07-19

### 新增：混沌爆破装置

- 新增 `dgmodules:chaos_crystal_breaker` 道具。
- 使用龙之研究神龙级材料，通过 DE 聚合合成制作。
- 右键混沌岛上14个 DE `GuardianCrystalEntity` 实体，或右键其承载方块安装；爆炸后暂时解除目标实体的防御，使普通攻击可以破坏水晶。
- 默认防御解除时间为 60 秒，可通过 `chaos_crystal_breaker.duration_ticks` 配置。
- 道具安装后消耗 1 个；创造模式不消耗。
- 新增英文、中文语言键、物品模型、32x32 物品纹理和聚合合成配方。
- 新增 `GuardianCrystalEntity` 的破防 mixin，直接清空并锁定目标实体护盾，不调用 DE 会产生 Minecraft 粒子的 `destabilize()`。
- 修复碰撞箱未被客户端识别时无法安装的问题：增加实体交互事件、服务端实体视线解析、承载方块定位和右键空气备用入口。

### 重做：混沌爆破装置奇点爆破流程

- 改为将破防器安装到目标实体下方的方块锚点，默认倒计时 5 秒后触发爆炸。
- 爆炸使用 DE 的 `DEDamage.guardian` 混沌龙伤害源，在小范围内伤害生物。
- 爆炸时只开启被安装目标 `GuardianCrystalEntity` 的临时防御解除状态，不再处理混沌岛中央的 `TileChaosCrystal`。
- 新增 S2C 奇点扩散特效和自定义顶点渲染器，不调用 Minecraft 粒子系统。
- 新增倒计时、范围、伤害、防御时长和特效时长配置项。

### 优化：混沌爆破装置倒计时与配方

- 移除 actionbar/GUI 读秒，改为在对应 `GuardianCrystalEntity` 正上方渲染世界空间倒计时数字。
- 将物品显示名改为“混沌爆破装置”（英文：`Chaos Blast Device`）。
- 将聚合配方中的 `draconicevolution:chaotic_core` 替换为 `draconicevolution:small_chaos_frag`（微小混沌碎片）。

### 调整：混沌爆破装置合成材料

- 将聚合配方中的 `draconicevolution:chaotic_energy_core` 替换为 `draconicevolution:draconic_energy_core`。
- 将原伤害模块催化剂替换为 TNT，并通过 1 个催化剂位加 3 个注入材料位配置为总计 4 个 TNT。

### 新增：微小混沌碎片爆炸转化

- 将龙之心、下界之星和钻石以掉落物形式投入同一次爆炸影响范围后，消耗各 1 个并生成 2 个微小混沌碎片。
- 新增 JEI 自定义分类，显示三种材料、爆炸转化条件和 2 个微小混沌碎片产物。

### 新增：模块进度树

- 为全部 24 个 DGModules 模块添加 Minecraft 进度，按飞龙、神龙、混沌科技等级分层。
- 对混沌激光、天灾箭矢、相位护盾、压缩混沌伤害等高强度模块使用 challenge 框架和更醒目的提示文本。
- 增加模块总览根进度与科技等级节点，所有进度通过获得对应模块自动触发。
- 新增独立的 DGmodules 进度标签根节点，进入世界后即可在进度界面看到专属模块科技树。

### 修复：模块进度标签未加载

- 修正 Minecraft 1.21.1 的进度资源目录为 `data/dgmodules/advancement`（单数），避免资源管理器静默跳过整棵自定义进度树。

### 新增：危险模块创造模式影响开关

- 新增服务端配置 `dangerous_modules.affect_creative_players`，默认关闭。
- 配置关闭时，混沌龙激光模块和天灾箭矢模块的直接伤害、持续穿刺伤害、坍缩生命值削减和混沌龙爆炸伤害均不会影响创造模式玩家。

### 新增：护主模块

- 新增 `dgmodules:host_integrity_module`（显示名：护主模块），只保护安装它的单个 DE 宿主。
- 通过物品槽、Curios、ItemStack 组件和容器操作入口进行即时拦截，不使用 PlayerTick/EntityTick 轮询。
- 非所有者的宿主替换、取出、丢弃、组件/NBT 修改会在写入前被拒绝；玩家本人通过服务器操作包发起的正常操作仍可执行。
- 新增 `host_integrity` 配置段，可分别控制装备、手持、Curios、普通背包范围及拦截日志。

### 修复：护主模块与容器批量转移

- 修复容器 Shift-click 批量转移时，物品栏每次写入都触发完整宿主扫描，可能造成容器瞬间填充或批量操作异常的问题。
- 改为仅在受保护宿主实际进入、离开或组件发生结构变化时延迟重建绑定；普通容器批量转移不再触发全量扫描。

### 修复：护主模块生存物品栏点击复制

- 将完整的容器点击调用纳入玩家本人操作上下文，避免合法点击被写入拦截后出现槽位未清空、光标拿到宿主副本的情况。
- 同时兼容原版网络包和直接调用容器菜单的模组容器。

### 优化：稳定模块数量与唯一身份

- 稳定模块物品堆叠上限和宿主安装上限均固定为 1，避免一份物品或一个宿主出现多份稳定模块状态。
- 每个已安装的稳定模块实体拥有独立 UUID，并随 DE 宿主数据和网络同步持久化；旧存档缺少 UUID 时会自动补齐。
- 护主绑定记录稳定模块 UUID，并在当前受保护范围内发现复制出的重复身份时自动重新生成，降低复制、重载和跨容器移动导致的绑定错乱。

## 更新记录规则

- 每次代码、资源、配方或配置变更，都要在本文件新增日期条目。
- 条目应记录变更文件或注册名，以及会影响玩家行为的功能变化。
