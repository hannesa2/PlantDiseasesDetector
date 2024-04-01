#!/usr/bin/env bash

pwd

if [[ -z "$CRYPT_PASS" ]]
then
   read -sp 'Password: ' CRYPT_PASS
   if [[ -z "$CRYPT_PASS" ]]
   then
      echo "\$CRYPT_PASS Still empty"
      exit 1
   fi
else
   echo "\$CRYPT_PASS available"
fi

openssl version

pushd signing

# to encrypt
#openssl aes-256-cbc -salt -pbkdf2 -k "$CRYPT_PASS" -in ./signing/release.keystore -out ./signing/release.keystore.enc
#openssl aes-256-cbc -salt -pbkdf2 -k "$CRYPT_PASS" -in ~/.android/debug.keystore -out ./signing/debug.keystore.enc
#openssl aes-256-cbc -salt -pbkdf2 -k "$CRYPT_PASS" -in ./app/google-services.json -out ./app/google-services.json.enc
#openssl aes-256-cbc -salt -pbkdf2 -k "$CRYPT_PASS" -in ./signing/Surveilance-playstore.json -out ./signing/Surveilance-playstore.json.enc
#openssl aes-256-cbc -salt -pbkdf2 -k "$CRYPT_PASS" -in ./signing/keystore.properties -out ./signing/keystore.properties.enc

# shellcheck disable=SC2038
find . -name "*.keystore.enc" | xargs ls -la

# Ubuntu 18.04 (openssl 1.1.0g+) needs -md md5
# https://askubuntu.com/questions/1067762/unable-to-decrypt-text-files-with-openssl-on-ubuntu-18-04/1076708
echo release.keystore
openssl aes-256-cbc -d -pbkdf2 -k "$CRYPT_PASS" -in release.keystore.enc -out release.keystore
echo debug.keystore
openssl aes-256-cbc -d -pbkdf2 -k "$CRYPT_PASS" -in debug.keystore.enc -out debug.keystore

#echo google-services.json
#openssl aes-256-cbc -d -pbkdf2 -k "$CRYPT_PASS" -in ../app/google-services.json.enc -out ../app/google-services.json
#echo Surveilance-playstore.json
#openssl aes-256-cbc -d -pbkdf2 -k "$CRYPT_PASS" -in Surveilance-playstore.json.enc -out Surveilance-playstore.json
echo keystore.properties
openssl aes-256-cbc -d -pbkdf2 -k "$CRYPT_PASS" -in keystore.properties.enc -out keystore.properties

popd 1>/dev/null