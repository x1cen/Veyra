/*
 * This is the source code of tgnet library v. 1.1
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2015-2018.
 */

#include <sys/stat.h>
#include <unistd.h>
#include <errno.h>
#include <cstring>
#include "Config.h"
#include "ConnectionsManager.h"
#include "FileLog.h"
#include "BuffersStorage.h"

#ifdef ANDROID
#include <jni.h>
extern JavaVM *javaVm;

static JNIEnv *getConfigJNIEnv() {
    JNIEnv *env = nullptr;
    if (javaVm != nullptr) {
        if (javaVm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
            javaVm->AttachCurrentThread(&env, nullptr);
        }
    }
    return env;
}
#endif

Config::Config(int32_t instance, std::string fileName) {
    instanceNum = instance;
    configPath = ConnectionsManager::getInstance(instanceNum).currentConfigPath + fileName;
    backupPath = configPath + ".bak";
    FILE *backup = fopen(backupPath.c_str(), "rb");
    if (backup != nullptr) {
        if (LOGS_ENABLED) DEBUG_D("Config(%p, %s) backup file found %s", this, configPath.c_str(), backupPath.c_str());
        fclose(backup);
        remove(configPath.c_str());
        rename(backupPath.c_str(), configPath.c_str());
    }
}

NativeByteBuffer *Config::readConfig() {
    NativeByteBuffer *buffer = nullptr;
    FILE *file = fopen(configPath.c_str(), "rb");
    if (file != nullptr) {
        fseek(file, 0, SEEK_END);
        long fileSize = ftell(file);
        if (fseek(file, 0, SEEK_SET)) {
            if (LOGS_ENABLED) DEBUG_E("Config(%p, %s) failed fseek to begin, reopen it", this, configPath.c_str());
            fclose(file);
            file = fopen(configPath.c_str(), "rb");
        }
        uint32_t size = 0;
        size_t bytesRead = fread(&size, sizeof(uint32_t), 1, file);
        if (LOGS_ENABLED) DEBUG_D("Config(%p, %s) load, size = %u, fileSize = %u", this, configPath.c_str(), size, (uint32_t) fileSize);
        if (bytesRead > 0 && size > 0 && (int32_t) size < fileSize) {
            uint8_t *rawBytes = new uint8_t[size];
            if (fread(rawBytes, sizeof(uint8_t), size, file) == size) {
#ifdef ANDROID
                JNIEnv *env = getConfigJNIEnv();
                jbyteArray decryptedArray = nullptr;
                if (env != nullptr) {
                    jclass keyStoreClass = env->FindClass("org/veyra/client/VeyraKeyStore");
                    if (keyStoreClass != nullptr) {
                        jmethodID decryptMethod = env->GetStaticMethodID(keyStoreClass, "decryptSessionBytes", "([B)[B");
                        if (decryptMethod != nullptr) {
                            jbyteArray inputEncArray = env->NewByteArray(size);
                            if (inputEncArray != nullptr) {
                                env->SetByteArrayRegion(inputEncArray, 0, size, (jbyte *) rawBytes);
                                decryptedArray = (jbyteArray) env->CallStaticObjectMethod(keyStoreClass, decryptMethod, inputEncArray);
                                env->DeleteLocalRef(inputEncArray);
                            }
                        }
                        env->DeleteLocalRef(keyStoreClass);
                    }
                }
                if (decryptedArray != nullptr) {
                    uint32_t decLen = (uint32_t) env->GetArrayLength(decryptedArray);
                    buffer = BuffersStorage::getInstance().getFreeBuffer(decLen);
                    env->GetByteArrayRegion(decryptedArray, 0, decLen, (jbyte *) buffer->bytes());
                    env->DeleteLocalRef(decryptedArray);
                } else {
                    buffer = BuffersStorage::getInstance().getFreeBuffer(size);
                    memcpy(buffer->bytes(), rawBytes, size);
                }
#else
                buffer = BuffersStorage::getInstance().getFreeBuffer(size);
                memcpy(buffer->bytes(), rawBytes, size);
#endif
            }
            delete[] rawBytes;
        }
        fclose(file);
    }
    return buffer;
}

void Config::writeConfig(NativeByteBuffer *buffer) {
    if (buffer == nullptr) return;
    if (LOGS_ENABLED) DEBUG_D("Config(%p, %s) start write config", this, configPath.c_str());

    uint8_t *dataToWrite = buffer->bytes();
    uint32_t size = buffer->position();

#ifdef ANDROID
    jbyteArray encryptedArray = nullptr;
    jbyte *encBytes = nullptr;
    JNIEnv *env = getConfigJNIEnv();
    if (env != nullptr) {
        jclass keyStoreClass = env->FindClass("org/veyra/client/VeyraKeyStore");
        if (keyStoreClass != nullptr) {
            jmethodID encryptMethod = env->GetStaticMethodID(keyStoreClass, "encryptSessionBytes", "([B)[B");
            if (encryptMethod != nullptr) {
                jbyteArray plainArray = env->NewByteArray(size);
                if (plainArray != nullptr) {
                    env->SetByteArrayRegion(plainArray, 0, size, (jbyte *) dataToWrite);
                    encryptedArray = (jbyteArray) env->CallStaticObjectMethod(keyStoreClass, encryptMethod, plainArray);
                    env->DeleteLocalRef(plainArray);
                }
            }
            env->DeleteLocalRef(keyStoreClass);
        }
    }
    if (encryptedArray != nullptr) {
        size = (uint32_t) env->GetArrayLength(encryptedArray);
        encBytes = env->GetByteArrayElements(encryptedArray, nullptr);
        dataToWrite = (uint8_t *) encBytes;
    }
#endif

    FILE *file = fopen(configPath.c_str(), "rb");
    FILE *backup = fopen(backupPath.c_str(), "rb");
    bool error = false;
    bool hasBackupFile = false;
    if (file != nullptr) {
        if (backup == nullptr) {
            fclose(file);
            if (rename(configPath.c_str(), backupPath.c_str()) != 0) {
                if (LOGS_ENABLED) DEBUG_E("Config(%p) unable to rename file %s to backup file %s", this, configPath.c_str(), backupPath.c_str());
                error = true;
            } else {
                hasBackupFile = true;
            }
        } else {
            fclose(file);
            fclose(backup);
            remove(configPath.c_str());
        }
    }
    if (error) {
#ifdef ANDROID
        if (encryptedArray != nullptr && encBytes != nullptr) {
            env->ReleaseByteArrayElements(encryptedArray, encBytes, JNI_ABORT);
            env->DeleteLocalRef(encryptedArray);
        }
#endif
        return;
    }
    file = fopen(configPath.c_str(), "wb");
    if (chmod(configPath.c_str(), 0660)) {
        if (LOGS_ENABLED) DEBUG_E("Config(%p, %s) chmod failed", this, configPath.c_str());
    }
    if (file == nullptr) {
        if (LOGS_ENABLED) DEBUG_E("Config(%p, %s) unable to open file for writing", this, configPath.c_str());
#ifdef ANDROID
        if (encryptedArray != nullptr && encBytes != nullptr) {
            env->ReleaseByteArrayElements(encryptedArray, encBytes, JNI_ABORT);
            env->DeleteLocalRef(encryptedArray);
        }
#endif
        return;
    }
    if (fwrite(&size, sizeof(uint32_t), 1, file) == 1) {
        if (fwrite(dataToWrite, sizeof(uint8_t), size, file) != size) {
            if (LOGS_ENABLED) DEBUG_E("Config(%p, %s) failed to write config data to file", this, configPath.c_str());
            error = true;
        }
    } else {
        if (LOGS_ENABLED) DEBUG_E("Config(%p, %s) failed to write config size to file", this, configPath.c_str());
        error = true;
    }
    if (fflush(file)) {
        if (LOGS_ENABLED) DEBUG_E("Config(%p, %s) fflush failed", this, configPath.c_str());
        error = true;
    }
    int fd = fileno(file);
    if (fd == -1) {
        if (LOGS_ENABLED) DEBUG_E("Config(%p, %s) fileno failed", this, configPath.c_str());
        error = true;
    } else {
        if (LOGS_ENABLED) DEBUG_D("Config(%p, %s) fileno = %d", this, configPath.c_str(), fd);
    }
    if (fd != -1 && fsync(fd) == -1) {
        if (LOGS_ENABLED) DEBUG_E("Config(%p, %s) fsync failed", this, configPath.c_str());
        error = true;
    }
    if (fclose(file)) {
        if (LOGS_ENABLED) DEBUG_E("Config(%p, %s) fclose failed", this, configPath.c_str());
        error = true;
    }

#ifdef ANDROID
    if (encryptedArray != nullptr && encBytes != nullptr) {
        env->ReleaseByteArrayElements(encryptedArray, encBytes, JNI_ABORT);
        env->DeleteLocalRef(encryptedArray);
    }
#endif

    if (error) {
        if (LOGS_ENABLED) DEBUG_E("Config(%p, %s) failed to write config", this, configPath.c_str());
        if (remove(configPath.c_str())) {
            if (LOGS_ENABLED) DEBUG_E("Config(%p, %s) remove config failed", this, configPath.c_str());
        }
    } else {
        if (hasBackupFile && remove(backupPath.c_str())) {
            if (LOGS_ENABLED) DEBUG_E("Config(%p, %s) remove backup failed, %s", this, backupPath.c_str(), strerror(errno));
        }
    }
    if (!error) {
        if (LOGS_ENABLED) DEBUG_D("Config(%p, %s) config write ok", this, configPath.c_str());
    }
}
