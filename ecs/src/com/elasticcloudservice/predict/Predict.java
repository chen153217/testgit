package com.elasticcloudservice.predict;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Predict {

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    /**
     *
     * @param ecsContent  各个规格的虚拟机的历史请求数据
     * @param inputContent 输入文件中的数据
     * @return  返回最终预测的结果
     */
    public static String[] predictVm(String[] ecsContent, String[] inputContent) {

        /** =========do your work here========== **/

        int lastDay=5;
        List<String> resultContents=new LinkedList<String>();//返回预测的结果
        List<Integer> flavorID = new ArrayList<Integer>(ecsContent.length);//将ecsContent中每行的flavorID保存下来
        List<String> createDate = new ArrayList<String>(ecsContent.length);//记录 escContent中每行的日期
        List[] flavorHistory=null;//记录15种虚拟机规格，每种虚拟机规格一段时间内的请求情况存放在对应的List中
        Map<Integer,Integer> flavorFuture;

        int[] physicsInfo = new int[3];//物理服务器资源信息，包含CPU核数，内存以及硬盘大小
        int[] flavorsToPredict;//需要预测的虚拟机型号
        //int[][] flavorsInfo;//各个虚拟机的规格
        int predictDateLength=0;//需要预测的时间长度(以天为单位）
        String referenceItem;//放置策略的参考因素，CPU or MEM

        //对历史请求数据escContent进行处理，获取flavorID,createDate和flavors
        String flavorName;
        String createTime;
        String[] array;
        for (int i = 1; i < ecsContent.length; i++) {

            if (ecsContent[i].contains("\t")
                    && ecsContent[i].split("\t").length == 3) {
                array = ecsContent[i].split("\t");
                flavorName = array[1];
                createTime = array[2];
                if (Integer.valueOf(flavorName.substring(6)) <= 15) {
                    flavorID.add(Integer.valueOf(flavorName.substring(6)));
                    createDate.add(createTime.split("\\s+")[0]);
                }
            }
        }
        try {
            flavorHistory = historyData(flavorID, createDate,lastDay);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        //对输入文件中的内容进行处理,提取出physicsInfo，flavorsToPredict，predictDateLength和referenceItem
        String str = inputContent[0];//
        String[] strs = str.split("\\s+");
        for (int j = 0; j < 3; j++)
            physicsInfo[j] = Integer.valueOf(strs[j]);

        int flavorNum = Integer.valueOf(inputContent[2]);
        flavorsToPredict = new int[flavorNum];
       // flavorsInfo = new int[flavorNum][3];
       int[][] flavorsInfo={
                {1,1024},
                {1,2048},
                {1,4096},
                {2,2048},
                {2,4096},
                {2,8129},
                {4,4096},
                {4,8129},
                {4,16384},
                {8,8192},
                {8,16384},
                {8,32768},
                {16,16384},
                {16,32768},
                {16,65536}
        };

        for (int j = 0; j < flavorNum; j++) {
            str = inputContent[j + 3];
            strs = str.split("\\s+");
            flavorsToPredict[j] =Integer.valueOf(strs[0].substring(6));
  //          flavorsToPredict[j] = flavorsInfo[j][0] = Integer.valueOf(strs[0].substring(6));
 //           flavorsInfo[j][1] = Integer.valueOf(strs[1]);
//            flavorsInfo[j][2] = Integer.valueOf(strs[2]);
        }

        referenceItem = inputContent[4 + flavorNum];

        try {
            Date beginDate = sdf.parse(inputContent[6 + flavorNum].split("\\s+")[0]);
            Date endDate = sdf.parse(inputContent[7 + flavorNum].split("\\s+")[0]);
            predictDateLength = (int) ((endDate.getTime() - beginDate.getTime()) / (1000 * 60 * 60 * 24));
        } catch (ParseException e) {
            e.printStackTrace();
        }

        flavorFuture=predictFlavor(flavorHistory,flavorsToPredict,predictDateLength,lastDay);
        int predictFlavorNum=0;
        for(Map.Entry<Integer,Integer> map:flavorFuture.entrySet()){
            predictFlavorNum+=map.getValue();
        }
        resultContents.add(String.valueOf(predictFlavorNum));
        String s="";
        for(Map.Entry<Integer,Integer> map:flavorFuture.entrySet()){
                s+="flavor"+map.getKey()+" "+map.getValue()+"\r\n";
        }
        resultContents.add(s);

        List<Map<Integer,Integer>> physicsList=putFlavorsToPhysics(flavorFuture,flavorsInfo,physicsInfo,referenceItem);
        resultContents.add(String.valueOf(physicsList.size()));
        int num=1;
        s="";
        for(Map<Integer,Integer> perPhysics:physicsList){
            s="";
            s+=num+" ";
            for(Map.Entry<Integer,Integer> perFlavor:perPhysics.entrySet()){
               s+="flavor"+perFlavor.getKey()+" "+perFlavor.getValue()+" ";
            }
            resultContents.add(s);
        }

        return resultContents.toArray(new String[resultContents.size()]);
    }

//    /**
//     *
//     * @param flavorID  历史请求数据每行的虚拟机ID
//     * @param createDate　历史请求数据每行中的请求时间
//     * @return      处理后的各种规格的flavors的历史请求数据，放到List数组中，每行对应一种规格
//     * @throws ParseException
//     */
//    private static List[] historyData(List<Integer> flavorID, List<String> createDate) throws ParseException {
//
//        List<Integer>[] flavors = new ArrayList[15];//15种虚拟机规格
//        for (int i = 0; i < 15; i++)
//            flavors[i] = new ArrayList<Integer>();//每种虚拟机规格一段时间内的请求情况存放在对应的List中
//
//        int size = flavorID.size();
//        Date beginDate = sdf.parse(createDate.get(0));
//        Date endDate = sdf.parse(createDate.get(size - 1));
//        Date currentDate;
//        int betweenDate = (int) ((endDate.getTime() - beginDate.getTime()) / (1000 * 60 * 60 * 24));
//        int benginNum=betweenDate%7;
//        int value = 0;
//
//        for (int i = 0; i < betweenDate / 7; i++) {
//            for (int j = 0; j < 15; j++) {
//                flavors[j].add(0);
//            }
//        }
//        for (int i = 0; i < size; i++) {
//            currentDate = sdf.parse(createDate.get(i));
//            betweenDate = (int) ((currentDate.getTime() - beginDate.getTime()) / (1000 * 60 * 60 * 24))-benginNum-1;//保证从开始记录的那天到endDate正好是7的倍数
//            if (betweenDate >= 0) {
//                value = flavors[flavorID.get(i) - 1].get(betweenDate / 7);
//                flavors[flavorID.get(i) - 1].set(betweenDate / 7, 1 + value);
//            }
//        }
//        return flavors;
//    }

    /**
     *
     * @param flavorID  历史请求数据每行的虚拟机ID
     * @param createDate　历史请求数据每行中的请求时间
     * @return      处理后的各种规格的flavors的历史请求数据，放到List数组中，每行对应一种规格
     * @throws ParseException
     */
    private static List[] historyData(List<Integer> flavorID, List<String> createDate,int n) throws ParseException {

        List<Integer>[] flavors = new ArrayList[15];//15种虚拟机规格
        for (int i = 0; i < 15; i++)
            flavors[i] = new ArrayList<Integer>();//每种虚拟机规格一段时间内的请求情况存放在对应的List中

        int size = flavorID.size();
        Date beginDate = sdf.parse(createDate.get(0));
        Date endDate = sdf.parse(createDate.get(size - 1));
        Date currentDate;
        int betweenDate = (int) ((endDate.getTime() - beginDate.getTime()) / (1000 * 60 * 60 * 24));
        int benginNum=betweenDate%n;
        int value = 0;

        for (int i = 0; i < betweenDate / n; i++) {
            for (int j = 0; j < 15; j++) {
                flavors[j].add(0);
            }
        }
        for (int i = 0; i < size; i++) {
            currentDate = sdf.parse(createDate.get(i));
            betweenDate = (int) ((currentDate.getTime() - beginDate.getTime()) / (1000 * 60 * 60 * 24))-benginNum-1;//保证从开始记录的那天到endDate正好是7的倍数
            if (betweenDate >= 0) {
                value = flavors[flavorID.get(i) - 1].get(betweenDate / n);
                flavors[flavorID.get(i) - 1].set(betweenDate /n, 1 + value);
            }
        }
        return flavors;
    }

    /**
     *
     * @param historyFlavors  处理后的各种规格的flavors的历史请求数据
     * @param flavorsID       需要预测的虚拟机的ID
     * @param betweenDate     需要预测的时间跨度
     * @return     map中的key-value分别对应flavorID-num,也即未来一段时间需要预测的各种ID的需求量
     */
    private static Map<Integer, Integer> predictFlavor(List<Integer>[] historyFlavors, int[] flavorsID, int betweenDate,int n){
        Map<Integer, Integer> predictCount = new HashMap<>();
        for(int id : flavorsID) {
            int count = predictFlavor(historyFlavors[id - 1], betweenDate,n);
            predictCount.put(id, count);
        }
        return predictCount;
    }


    private static int predictFlavor(List<Integer> list,int betweenDate,int n){

        int len=list.size();
        //去噪
//        for(int i=1;i<len-1;i++){
//            if(list.get(i)>2*(list.get(i-1)+list.get(i+1))&&list.get(i)>10)
//                list.set(i,list.get(i-1)+list.get(i+1));
//        }
//
//        if(list.get(len-1)>2*(list.get(len-2)+list.get(len-3))&&list.get(len-1)>10){
//            list.set(len-1,list.get(len-2)+list.get(len-3));
//        }
        float sum=0;
        for(int i=2;i<len-2;i++){
        //    sum=list.get(i-3)+list.get(i-2)+list.get(i-1);
       //     if(list.get(i)>sum&&list.get(i)>5)
     //           list.set(i,Math.round(sum/3));
            sum=list.get(i-2)+list.get(i-1)+list.get(i)+list.get(i+1)+list.get(i+2);
            list.set(i,Math.round(sum/5));
        }
        sum=list.get(len-4)+list.get(len-3)+list.get(len-2)+list.get(len-1);
        list.set(len-2,Math.round(sum/4));
        sum=list.get(len-3)+list.get(len-2)+list.get(len-1);
        list.set(len-1,Math.round(sum/3));
        float resultNum=0;

        float[] w=new float[len];  //每一周的权重
        w[len-1]=0.31f;  //最接近的一个日期分配为0.6权重,取0.6主要是防止出现只有两周时出现0.5  0.5的权重比例
        w[len-2]=0.27f;
        w[len-3]=0.2f;
        w[len-4]=0.12f;
        w[len-5]=0.1f;

//        float lest=0.25f;
//        for(int i=n-4;i>0;i--){
//            w[i]=lest*0.4f;
//            lest-=w[i];  //每一次分配过后权重还剩余多少
//        }
//        w[0]=lest;  //最后剩余的全部分配给w[0];
        float cur=0;
        for(int j=0;j<=betweenDate/n;j++) {
            for (int i = 0; i < len; i++) {
                cur += w[i] * list.get(i+j);
            }
            list.add(Math.round(cur));
        }
        for(int j=0;j<betweenDate/n;j++){
            resultNum+=list.get(len+j);
        }
        resultNum+=list.get(list.size()-1)*(betweenDate%n)/n;
        return Math.round(resultNum);
    }

//    /**
//     *
//     * @param list   一种虚拟机的历史请求记录
//     * @param betweenDate  需要预测的时间跨度
//     * @return  预测的虚拟机的数目
//     */
//    private static int predictFlavor(List<Integer> list, int betweenDate) {
////        Double[] fs = new Double[list.size()];
//        int length=list.size();
//        double[] temp = new double[length];
//        //list.toArray(fs);
//        double sum = 0.0, average, result = 0.0;
//        for(Integer var : list)
//            sum += var;
//        average = sum / list.size();
//        for(int i = 0; i < length; ++i) {
//            if(list.get(i) > 2 * average)
//                list.set(i,(int)average/2);
//            temp[i] = list.get(i);
//        }
//        int i;
//        for(i = length +1; i < length + 1 + betweenDate / 7; ++i)
//            result += gm(temp, i);
//        result+=(gm(temp,i)*betweenDate%7)/7;
//        return (int)result;
//    }

    public static double gm(double[] fs, int T) {
        // 预测模型函数
        int size = fs.length;
        int tsize = fs.length - 1;
        double[] arr = fs;// 原始数组
        double[] arr1 = new double[size];// 经过一次累加数组
        double sum = 0;
        for (int i = 0; i < size; i++) {
            sum += arr[i];
            arr1[i] = sum;
        }
        double[] arr2 = new double[tsize];// arr1的紧邻均值数组
        for (int i = 0; i < tsize; i++) {
            arr2[i] = (double) (arr1[i] + arr1[i + 1]) / 2;
        }
        /*
         *
         * 下面建立 向量B和YN求解待估参数向量， 即求参数a,b
         */
        /*
         * 下面建立向量B,相当于一个二维数组。
         */
        double[][] B = new double[tsize][2];
        for (int i = 0; i < tsize; i++) {
            for (int j = 0; j < 2; j++) {
                if (j == 1)
                    B[i][j] = 1;
                else
                    B[i][j] = -arr2[i];
            }
        }
        /*
         * 下面建立向量YN
         */
        double[][] YN = new double[tsize][1];
        for (int i = 0; i < tsize; i++) {
            for (int j = 0; j < 1; j++) {
                YN[i][j] = arr[i + 1];
            }
        }
        /*
         * B的转置矩阵BT
         */
        double[][] BT = new double[2][tsize];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < tsize; j++) {
                BT[i][j] = B[j][i];
            }
        }
        /*
         * 将BT和B的乘积所得到的结果记为数组B2T,则B2T是一个2*2的矩阵
         */
        double[][] B2T = new double[2][2];
        for (int i = 0; i < 2; i++) {// rows of BT
            {
                for (int j = 0; j < 2; j++)// cloums of B
                {
                    for (int k = 0; k < tsize; k++)// cloums of BT=rows of B
                    {
                        B2T[i][j] = B2T[i][j] + BT[i][k] * B[k][j];
                    }
                }

            }
        }
        /* 下面求B2T的逆矩阵，设为B_2T */
        double[][] B_2T = new double[2][2];
        B_2T[0][0] = (1 / (B2T[0][0] * B2T[1][1] - B2T[0][1] * B2T[1][0]))
                * B2T[1][1];
        B_2T[0][1] = (1 / (B2T[0][0] * B2T[1][1] - B2T[0][1] * B2T[1][0]))
                * (-B2T[0][1]);
        B_2T[1][0] = (1 / (B2T[0][0] * B2T[1][1] - B2T[0][1] * B2T[1][0]))
                * (-B2T[1][0]);
        B_2T[1][1] = (1 / (B2T[0][0] * B2T[1][1] - B2T[0][1] * B2T[1][0]))
                * B2T[0][0];
        /*
         * 根据以上所求的各已知量下面求待估参数的未知量a和b，待估向量矩阵等于B_2T*BT*YN
         * 下面我们分别求这些矩阵的乘积，首先求B_2T*BT，令B_2T*BT的乘积为A矩阵，则A就是一个2*5的矩阵
         */
        /*
         * 下面先求A矩阵
         */
        double[][] A = new double[2][tsize];
        for (int i = 0; i < 2; i++) {// rows of B_2T
            {
                for (int j = 0; j < tsize; j++)// cloums of BT
                {
                    for (int k = 0; k < 2; k++)// cloums of B_2T=rows of BT
                    {
                        A[i][j] = A[i][j] + B_2T[i][k] * BT[k][j];
                    }
                }

            }
        }
        /*
         * 下面求A和YN矩阵的乘积，令乘积为C矩阵，则C就是一个2*1的矩阵
         */
        double[][] C = new double[2][1];
        for (int i = 0; i < 2; i++) {// rows of A

            {
                for (int j = 0; j < 1; j++)// cloums of YN
                {
                    for (int k = 0; k < tsize; k++)// cloums of A=rows of YN
                    {
                        C[i][j] = C[i][j] + A[i][k] * YN[k][j];
                    }
                }

            }
        }
        /* 根据以上所得则a=C[0][0],b=C[1][0]; */
        double a = C[0][0], b = C[1][0];
        int i = T;// 读取一个数值
        double Y = (arr[0] - b / a) * Math.exp(-a * (i + 1)) - (arr[0] - b / a)
                * Math.exp(-a * i);

        return Y;
    }


    /**
     *
     * @param flavors  要求预测的各种虚拟机的使用需求量
     * @param flavorsInfo  每种虚拟机对应的参数，包括ID，CPU及内存的容量配置
     * @param physicsInfo　　一台物理机对应的配置容量信息，包含CPU核数，内存以及硬盘大小
     * @param referenceItem　CPU或者MEM，放置时优先考虑的因素
     * @return  　一个Map对应一台物理机放置的虚拟机的规格及数量
     */
    private static List<Map<Integer,Integer>> putFlavorsToPhysics(Map<Integer,Integer> flavors,int[][] flavorsInfo,int[] physicsInfo,String referenceItem){
        int[] flavorID=new int[15];   //记录虚拟机ID
        for(int i=0;i<15;i++)
            flavorID[i]=i+1;
        //如果是考虑内存优先，则对内存进行排序
        if(referenceItem.equals("MEM")){
            //随着虚拟机ID增大内存大致也是上升的，用冒泡排序交换次数较少
            for(int i=0;i<15;i++){
                for(int j=i+1;j<15;j++){
                    if(flavorsInfo[i][1]>flavorsInfo[j][1]){
                        int temp1=flavorsInfo[i][1];
                        int temp2=flavorsInfo[j][0];
                        int temp3=flavorID[i];
                        flavorsInfo[i][1]=flavorsInfo[j][1];
                        flavorsInfo[i][0]=flavorsInfo[j][0];
                        flavorID[i]=flavorID[j];
                        flavorsInfo[j][1]=temp1;
                        flavorsInfo[j][0]=temp2;
                        flavorID[j]=temp3;
                    }
                }
            }
        }
        //如果是考虑CPU优先，则对CPU核数多少进行排序
        if(referenceItem.equals("CPU")){
            for(int i=0;i<15;i++){
                for(int j=i+1;j<15;j++){
                    if(flavorsInfo[i][0]>flavorsInfo[j][0]){
                        int temp1=flavorsInfo[i][1];
                        int temp2=flavorsInfo[j][0];
                        int temp3=flavorID[i];
                        flavorsInfo[i][1]=flavorsInfo[j][1];
                        flavorsInfo[i][0]=flavorsInfo[j][0];
                        flavorID[i]=flavorID[j];
                        flavorsInfo[j][1]=temp1;
                        flavorsInfo[j][0]=temp2;
                        flavorID[j]=temp3;
                    }
                }
            }
        }
        int currentCPU=physicsInfo[0];  //目前正在部署的服务器剩余的CPU核数
        int currentMEM=physicsInfo[1]*1024;   //目前正在部署的服务器剩余的内存
        int count=0;                     //记录各种虚拟机加起来的总数量
        List<Map<Integer, Integer>> serverList=new ArrayList<Map<Integer,Integer>>();  //存放最终结果
        int[] currentFlavorCount=new int[15];   //用来记录每种虚拟机的数量

        //遍历Map记录各规格的虚拟机数量
        for(Map.Entry<Integer,Integer> aKindOfFlavor:flavors.entrySet()){
            currentFlavorCount[aKindOfFlavor.getKey()-1]=aKindOfFlavor.getValue();   //记录当前规格虚拟机数量
            count+=aKindOfFlavor.getValue();     //虚拟机总数累加
        }

        //一直循环直到count=0，即虚拟机全部放置完毕
        while(count!=0){
            Map<Integer, Integer> aServer=new HashMap<Integer, Integer>();
            currentCPU=physicsInfo[0];
            currentMEM=physicsInfo[1]*1024;
            for(int i=14;i>=0;i--){    //先放排序后在后面规格的虚拟机，因为后面规格的虚拟机的CPU和内存较大
                if(currentFlavorCount[flavorID[i]-1]!=0 && currentCPU>=flavorsInfo[flavorID[i]-1][0] && currentMEM>=flavorsInfo[flavorID[i]-1][1]){
                    int n=currentFlavorCount[flavorID[i]-1];
                    for(int j=0;j<n;j++){
                        if(currentCPU>=flavorsInfo[flavorID[i]-1][0] && currentMEM>=flavorsInfo[flavorID[i]-1][1]){  //CPU和内存大小满足，可以放置
                            //减去相应的容量
                            currentCPU-=flavorsInfo[flavorID[i]-1][0];
                            currentMEM-=flavorsInfo[flavorID[i]-1][1];
                            currentFlavorCount[flavorID[i]-1]--;
                            count--;

                            //先判断该服务器上是否已有此种型号的虚拟机，有则+1，没有则置1
                            if(aServer.containsKey(flavorID[i])){
                                aServer.put(flavorID[i],aServer.get(flavorID[i])+1);
                            }
                            else {
                                aServer.put(flavorID[i],1);
                            }
                        }
                    }
                }
            }
            //运行到这里说明目前服务器的容量已经不足或者虚拟机已经全部放置完毕
            serverList.add(aServer);
        }
        return serverList;
    }





}
