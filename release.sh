#!/bin/bash

## --------- 方法定义、引入

## 数组join成字符串
function join_by { local IFS="$1"; shift; echo "$*"; }

## 判断输入是否为整数
function isint () {
    # 输入为空，返回 0
    # 输入非整数，返回 0
    # 输入整数，返回 1
    if [ $# -lt 1 ]; then
        return 0
    fi
 
    if [[ $1 =~ ^-?[1-9][0-9]*$ ]]; then
        return 1
    fi
 
    if [[ $1 =~ ^0$ ]]; then
        return 1
    fi
 
    return 0
}

## --------- 解析脚本参数

## 约定第一个参数为打tag时的前缀
TAG_PERFIX="v"
if [ "z" != "z$1" ] ; then
    TAG_PERFIX="$1"
fi

## --------- 开始

WR=`dirname $0`

cd $WR

FORCE=
gstat=$(git status| grep "nothing to commit, working tree clean")
if [ "z" == "z$gstat" ] ; then
    echo " -- It's a not clean work tree!!!"
    echo " -- The uncommited files will be ignore!!!"
    read -p "Force release? (y/N) (default: N):" FORCE
    echo "..."
    if [ "zy" != "z${FORCE}" ] ; then
        exit 0;
    fi
fi

VERSION_CURR=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

read -p "enter release version (default: ${VERSION_CURR}):" VERSION_TAG
if [ "z" == "z${VERSION_TAG}" ] ; then
    VERSION_TAG=${VERSION_CURR}
fi

VERSION_NEW=${VERSION_TAG}
VERSION_DEPLOY="${VERSION_TAG}"
VERSION_TAG="${TAG_PERFIX}${VERSION_TAG}"

echo "------"
echo "Fetch current version in pom.xml: ${VERSION_CURR}"
echo "Detect release version: ${VERSION_DEPLOY}"
echo "Detect new tag name: ${VERSION_TAG}"
echo "------"

if [ "z${VERSION_CURR}" != "${VERSION_NEW}" ] ; then
    echo "!!! we will change version in your maven project, do you accept this action?"
    read -p "Input any key to continue or CRTL + c to break this script... " XYZ
    mvn versions:set -DnewVersion="${VERSION_NEW}"
fi

read -p "Check info and input anything to continue OR ctrl+c to interupt this script ... : " XYZ
#exit 0

# 打上tag
git tag -m "Auto tag by script." "${VERSION_TAG}"
#git push origin "v${VERSION_TAG}"

# 清理本地安装
rm -fr ~/.m2/repository/com/github/kimffy24/ejoker*

rm -fr "$WR/target/checkout"
mkdir -p "$WR/target/checkout"
cd "$WR/target/checkout"

## 虽然这方法不怎么规范，但是从远程的源中clone一次是更加痛苦的事。
cp -a ../../.git ./
## 还原所有文件
git checkout -- .
git checkout "${VERSION_TAG}"

echo "mvn clean compile package deploy -P release"
#mvn clean compile package deploy -P release
RES=$?
if [ $RES -ne 0 ] ; then
    echo "Something wrong!!!"
    exit 1
fi

cd ../..


echo "Your can push tag to origin by using:"
echo "    git push origin \"${VERSION_TAG}\""
echo