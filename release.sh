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

VERSION_TAG=
VERSION_DEPLOY=

if [[ $VERSION_CURR =~ "-SNAPSHOT"$ ]] ; then
    
    array=(${VERSION_CURR//-SNAPSHOT/ })
    VERSION_TAG=${array[0]}

    read -p "enter release version (default: ${VERSION_TAG}):" VERSION_TAGZ
    if [ "z" != "z${VERSION_TAGZ}" ] ; then
        VERSION_TAG=${VERSION_TAGZ}
    fi

else

    read -p "enter release version (default: ${VERSION_CURR}):" VERSION_TAG
    if [ "z" == "z${VERSION_TAG}" ] ; then
        VERSION_TAG=${VERSION_CURR}
    fi

fi
VERSION_NEW=

typeset v_I=0
declare -a vs
array2=(${VERSION_TAG//'.'/ })
for i in ${array2[@]} ; do
    vs[$v_I]=$i
    ((v_I++))
done
((v_I--))
NN=${vs[$v_I]}
isint $NN
RES=$?
if [ $RES -gt 0 ] ; then
    ((NN++))
    vs[$v_I]=$NN
    VERSION_NEW=$(join_by '.' ${vs[*]})
    VERSION_NEW="${VERSION_NEW}-SNAPSHOT"
    read -p "Your can specify a new version (default: ${VERSION_NEW}): " VERSION_NEWZ
    if [ "z" != "z${VERSION_NEWZ}" ] ; then
        VERSION_NEW=${VERSION_NEWZ}
    fi
else
    read -p "!!! Your must specify a new version: " VERSION_NEW
fi

if [ "z" == "z${VERSION_NEW}" ] ; then
    echo -n "Cannot detect a new develop version!!!"
    exit 1
fi


VERSION_DEPLOY="${VERSION_TAG}"
VERSION_TAG="${TAG_PERFIX}${VERSION_TAG}"

echo "------"
echo "Fetch current version in pom.xml: ${VERSION_CURR}"
echo "Detect release version: ${VERSION_DEPLOY}"
echo "Detect new tag name: ${VERSION_TAG}"
echo "Detect new version after deploy: ${VERSION_NEW}"
echo "------"
read -p "Check info and input anything to continue OR ctrl+c to interupt this script ... : " XYZ
#exit 0

#mvn versions:set -DnewVersion="${VERSION_DEPLOY}"
#for i in `git ls-files|grep pom.xml` ; do
#    git add $i
#done
git tag -m "Auto tag by script." "${VERSION_TAG}"

#git push origin "v${VERSION_TAG}"


rm -fr "$WR/target/checkout"
mkdir -p "$WR/target/checkout"
cd "$WR/target/checkout"

## 虽然这方法不怎么规范，但是从远程的源中clone一次是更加痛苦的事。
cp -a ../../.git ./
## 还原所有文件
git checkout -- .
git checkout "${VERSION_TAG}"
mvn versions:set -DnewVersion="${VERSION_DEPLOY}"

mvn clean deploy -P release
RES=$?
if [ $RES -ne 0 ] ; then
    echo "Something wrong!!!"
    exit 1
fi

cd ../..
mvn versions:set -DnewVersion="${VERSION_NEW}"
for i in `git ls-files|grep pom.xml` ; do
   git add $i
done

echo "Your can push tag to origin by using:"
echo "    git push origin \"v${VERSION_TAG}\""
echo