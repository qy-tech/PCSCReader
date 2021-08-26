# Android PcscReader lib 使用说明

1. 导入依赖库

   在对应的 app 中的 build.gradle 文件中添加一下依赖

   ```groovy
       implementation files('libs/pcscReader-release.aar')
       implementation "com.jakewharton.timber:timber:4.7.1"
       implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.4.3'
       implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3"
       implementation 'com.gemalto.jp2:jp2-android:1.0'	
   ```

   

2. 创建SmartCardManager并设置监听

   ```kotlin
   override fun onActivityCreated(savedInstanceState: Bundle?) {
           super.onActivityCreated(savedInstanceState)
           //省略其他代码......
           smartCardManager = SmartCardManager(requireContext(), smartCardStatusListener)

       }
     private val smartCardStatusListener = object : SCardStatusChangeListener {
        override fun onSCardConnect(atr: String) {
            Timber.d("onSCardConnect atr is $atr")
            if (atr.isEmpty()) {
                return
            }
            smartCardManager.initCard(mrzDT3)

            smartCardManager.launch(Dispatchers.Main) {
                withContext(Dispatchers.IO) {
                    smartCardManager.readCard(EF.COM)
                }?.let {
                    Timber.d("onSCardConnect: ${EF.parsingCOM(it)}")
                }

                withContext(Dispatchers.IO) {
                    smartCardManager.readCard(EF.DG1)
                }?.let {
                    val dg1 = EF.parsingDG1(it)
                    viewModel.setPassportInfo(PCSCUtils.getPassportInfo(dg1))
                    Timber.d("onSCardConnect: $dg1")
                }

                withContext(Dispatchers.IO) {
                    smartCardManager.readCard(EF.DG2)
                }?.let {
                    val dg2 = EF.parsingDG2(it)
                    dataBinding.ivPassportAvatars.setImageBitmap(dg2)
                }
            }

        }

        override fun onSCardDisconnect() {
            Timber.d("onSCardDisconnect")
            viewModel.setPassportInfo(null)
            dataBinding.ivPassportAvatars.setImageResource(R.color.cardview_dark_background)
        }
    }
   
   
    override fun onResume() {
        super.onResume()
        smartCardManager.launch {
            smartCardManager.connectCardReader()
        }
    }

    override fun onStop() {
        super.onStop()
        smartCardManager.disConnectCardReader()
    }

    override fun onDestroy() {
        super.onDestroy()
        smartCardManager.release()
    }
   
   ```

3. PassportInfo 数据类介绍

   ```kotlin
   data class PassportInfo(
       var passType: String = "",//护照类型
       var passNumber: String = "",//卡号
       var numberCheck: String = "",//卡号校验位
       var country: String = "",//国籍
       var birthday: String = "",//生日
       var birthdayCheck: String = "",//生日校验
       var gender: String = "",//性别
       var dueDate: String = "",//护照到期时间校验位
       var dueCheck: String = "",//护照到期时间校验位
       var optionData: String = "",//可选择填充数据
       var checkDigit: String = "",//校验位数据
       var compositeCheckDigit: String = "",//综合校验位
       var fullName: String = "",//本国字符集的全名
       var otherName: String = "",//本国字符集的全名
       var personalNumber: String = "",//个人号码
       var yearMonthDay: String = "",//以 yyyymmdd 表示的出生日期
       var placeBirth: String = "",// 出生地
       var permanentAddress: String = "",// 永久地址
       var phoneNumber: String = "",// 电话
       var job: String = "",// 职业
       var jobTitle: String = "",// 职衔
       var personalResume: String = "",// 个人简历
       var issueAgency: String = "",//签发机构
       var issueDate: String = "",//签发日期
   ) {
       //获取 MRZ 信息
       fun getMRZInfo(): String {
           return "$passNumber$numberCheck$birthday$birthdayCheck$dueDate$dueCheck"
               .replace("<+".toRegex(), "")
       }
   }
   ```

   