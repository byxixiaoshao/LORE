package com.bicy.novel.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 加密工具类，使用 Android Keystore 保护敏感数据
 */
object CryptoUtils {
    private const val KEY_ALIAS = "novel_editor_key"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val TAG_LENGTH = 128
    
    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
    }
    
    /**
     * 获取或创建密钥
     */
    private fun getOrCreateKey(): SecretKey {
        return if (keyStore.containsAlias(KEY_ALIAS)) {
            (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        } else {
            createKey()
        }
    }
    
    /**
     * 创建新密钥
     */
    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEY_STORE
        )
        
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
    
    /**
     * 加密字符串
     * @param plainText 明文
     * @return 加密后的字符串（Base64编码），格式为：IV长度(4字节) + IV + 密文
     */
    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val iv = cipher.iv
            
            // 将 IV 长度、IV 和密文组合在一起
            val combined = ByteArray(4 + iv.size + encryptedBytes.size)
            combined[0] = (iv.size shr 24).toByte()
            combined[1] = (iv.size shr 16).toByte()
            combined[2] = (iv.size shr 8).toByte()
            combined[3] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 4, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, 4 + iv.size, encryptedBytes.size)
            
            return android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            // 加密失败时返回原文（向后兼容）
            return plainText
        }
    }
    
    /**
     * 解密字符串
     * @param encryptedText 加密后的字符串
     * @return 解密后的明文
     */
    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return ""
        
        try {
            val combined = android.util.Base64.decode(encryptedText, android.util.Base64.NO_WRAP)
            
            // 检查是否是加密数据（至少有4字节的IV长度）
            if (combined.size < 4) {
                // 可能是未加密的旧数据，直接返回
                return encryptedText
            }
            
            // 读取 IV 长度
            val ivLength = ((combined[0].toInt() and 0xFF) shl 24) or
                           ((combined[1].toInt() and 0xFF) shl 16) or
                           ((combined[2].toInt() and 0xFF) shl 8) or
                           (combined[3].toInt() and 0xFF)
            
            // 检查 IV 长度是否合理（GCM IV 通常是12字节）
            if (ivLength <= 0 || ivLength > 32 || combined.size < 4 + ivLength) {
                // 可能是未加密的旧数据，直接返回
                return encryptedText
            }
            
            // 提取 IV 和密文
            val iv = combined.copyOfRange(4, 4 + ivLength)
            val encryptedBytes = combined.copyOfRange(4 + ivLength, combined.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            // 解密失败时返回原文（向后兼容未加密的旧数据）
            return encryptedText
        }
    }
    
    /**
     * 检查字符串是否已加密
     */
    fun isEncrypted(text: String): Boolean {
        if (text.isEmpty()) return false
        
        try {
            val combined = android.util.Base64.decode(text, android.util.Base64.NO_WRAP)
            if (combined.size < 4) return false
            
            val ivLength = ((combined[0].toInt() and 0xFF) shl 24) or
                           ((combined[1].toInt() and 0xFF) shl 16) or
                           ((combined[2].toInt() and 0xFF) shl 8) or
                           (combined[3].toInt() and 0xFF)
            
            return ivLength > 0 && ivLength <= 32 && combined.size >= 4 + ivLength
        } catch (e: Exception) {
            return false
        }
    }
}
