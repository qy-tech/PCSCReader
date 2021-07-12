# Android PcscReader lib 使用说明

1. 导入依赖库

   在对应的 app 中的 build.gradle 文件中添加一下依赖

   ```groovy
       implementation files('libs/pcscReader-release.aar')
    		implementation 'com.gemalto.jp2:jp2-android:1.0'
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
           val result = smartCardManager.connectCardReader()
           if (!result) {
               Timber.e("connectCardReader failed ,try aging later")
               return
           }
       }
   private val smartCardStatusListener = object : SmartCardManager.SCardStatusChangeListener {
     			//放下卡片回调
           override fun onSCardConnect(atr: String) {
               Timber.d("onSCardConnect atr is $atr")
               if (atr.isEmpty()) {
                   return
               }
               // 使用 OCR识别到的机读区数据初始化护照
               val result = smartCardManager.initPassport(mrzDT3)
               if (!result) {
                   return
               }
               //显示进度条
               dataBinding.progress.visibility = View.VISIBLE
               GlobalScope.launch(Dispatchers.IO) {
                   //读区 DG1~DG2 和 DG11~DG12 分区数据
                   var data = smartCardManager.readingPassport(EF.COM)
                   val com = EF.parsingCOM(data)
                   Timber.d("EF.COM is $com")
                   data = smartCardManager.readingPassport(EF.DG1)
                   val mrzInfo = EF.parsingDG1(data)
                   Timber.d("EF.DG1 is $mrzInfo")
                   var passportInfo = PCSCUtils.getPassportInfo(mrzInfo)
                   data = smartCardManager.readingPassport(EF.DG2)
                   val bitmap = EF.parsingDG2(data)
                   if (bitmap != null) {
                       launch(Dispatchers.Main) {
                           dataBinding.ivPassportAvatars.setImageBitmap(bitmap)
                       }
                   }
                   data = smartCardManager.readingPassport(EF.DG11)
                   passportInfo = EF.parsingDG11(data, passportInfo)
                   data = smartCardManager.readingPassport(EF.DG12)
                   passportInfo = EF.parsingDG12(data, passportInfo)
                   launch(Dispatchers.Main) {
                       viewModel.setPassportInfo(passportInfo)
                       Timber.d("dismiss progress")
                       dataBinding.progress.visibility = View.GONE
                   }
               }
   
   
           }
     
   				//拿走卡片的回调
           override fun onSCardDisconnect() {
               Timber.d("onSCardDisconnect")
               viewModel.setPassportInfo(null)
             dataBinding.ivPassportAvatars.setImageResource(R.color.cardview_dark_background)
           }
       }
   
   override fun onDestroy() {
           super.onDestroy()
     			// 释放资源
           smartCardManager.destroyCardReader()
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

   