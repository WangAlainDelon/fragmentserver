//package com.wx.springboot.autoconfigure;
//
//import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.context.annotation.Configuration;
//
///**
// * 包扫描
// *
// * @author shuangfei.zhu@hand-china.com 2020/2/03 13:31
// */
//@Configuration
//@ComponentScan(basePackages = "org.hzero.fragment")
//public class FragmentAutoConfig {
//
//    @Bean
//    @ConditionalOnMissingBean
//    public FragmentInitializeConfig fragmentInitializeConfig() {
//        return new FragmentInitializeConfig();
//    }
//}