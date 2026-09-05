<template>
  <div class="wrapper">
    <header>
      <p>用户注册</p>
    </header>

    <ul class="form-box">
      <li class="form-item avatar-item">
        <label for="avatarFile" class="form-item-title">头像：</label>
        <div class="form-item-content">
          <div id="image-upload-area" class="image-upload-area" @click="triggerFileInput">
            <img :src="avatar" class="image-preview" alt="用户头像预览" />
            <span class="change-avatar">更换</span>
            <input id="avatarFile" type="file" accept="image/jpeg,image/png,image/webp" class="file-input" @change="handleFileChange" ref="fileInput" />
          </div>
          <p class="avatar-hint">不上传将使用默认头像</p>
        </div>
      </li>
      <li class="form-item">
        <label for="username" class="form-item-title">用户名称：</label>
        <div class="form-item-content">
          <input
            id="username"
            type="text"
            v-model="user.username"
            placeholder="用户名称"
            maxlength="20"
          />
          <p class="word-count">{{ user.username.length }}/20</p>
        </div>
      </li>
      <li class="form-item">
        <label for="familyName" class="form-item-title">姓：</label>
        <div class="form-item-content">
          <input
            id="familyName"
            type="text"
            v-model="user.familyName"
            placeholder="姓"
          />
        </div>
      </li>
      <li class="form-item">
        <label for="givenName" class="form-item-title">名：</label>
        <div class="form-item-content">
          <input
            id="givenName"
            type="text"
            v-model="user.givenName"
            placeholder="名"
          />
        </div>
      </li>
      <li class="form-item">
        <label for="phone" class="form-item-title">手机号码：</label>
        <div class="form-item-content">
          <input id="phone" type="text" v-model="user.phone" placeholder="手机号码" />
        </div>
      </li>
      <li class="form-item">
        <label for="password" class="form-item-title">密码：</label>
        <div class="form-item-content password-field">
          <input id="password" :type="showPassword ? 'text' : 'password'" v-model="user.password" placeholder="密码" />
          <button type="button" :aria-label="showPassword ? '隐藏密码' : '显示密码'" @click="showPassword = !showPassword"><i :class="showPassword ? 'fas fa-eye-slash' : 'fas fa-eye'"></i></button>
        </div>
      </li>
      <li class="form-item">
        <label for="confirmPassword" class="form-item-title">确认密码：</label>
        <div class="form-item-content password-field">
          <input id="confirmPassword" :type="showConfirmPassword ? 'text' : 'password'" v-model="confirmPassword" placeholder="确认密码" />
          <button type="button" :aria-label="showConfirmPassword ? '隐藏密码' : '显示密码'" @click="showConfirmPassword = !showConfirmPassword"><i :class="showConfirmPassword ? 'fas fa-eye-slash' : 'fas fa-eye'"></i></button>
        </div>
      </li>
      <li class="form-item">
        <label for="useremail" class="form-item-title">邮箱：</label>
        <div class="form-item-content">
          <input id="useremail" type="text" v-model="user.useremail" placeholder="请输入邮箱" />
        </div>
      </li>
      <li class="form-item gender-item">
        <div class="form-item-title">性别：</div>
        <div class="form-item-content">
          <input type="radio" v-model="user.gender" value="男" id="male" />
          <label for="male">男</label>
          <input type="radio" v-model="user.gender" value="女" id="female" />
          <label for="female">女</label>
        </div>
      </li>
    </ul>

    <div class="button-register">
      <button :disabled="submitting" @click="register">{{ submitting ? '正在注册…' : '注册' }}</button>
    </div>

    <div v-if="messageBoxVisible" class="message-box-overlay">
      <div class="message-box">
        <p>{{ messageBoxMessage }}</p>
        <button @click="closeMessageBox">确定</button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';
import { toast } from '../utils/toast';
import { DEFAULT_USER_AVATAR } from '../utils/profileDefaults';

export default {
  name: 'Register',
  setup() {
    const router = useRouter();
    const user = reactive({
      phone: '',
      password: '',
      username: '',
      useremail: '',
      gender: '男',
      familyName: '',
      givenName: ''
    });
    const confirmPassword = ref('');
    const showPassword = ref(false);
    const showConfirmPassword = ref(false);
    const avatar = ref(DEFAULT_USER_AVATAR);
    const uploadedFile = ref(null);
    const fileInput = ref(null);
    const submitting = ref(false);

    // 消息框状态
    const messageBoxVisible = ref(false);
    const messageBoxMessage = ref('');

    // 显示消息框
    const showMessageBox = (message) => {
      messageBoxMessage.value = message;
      messageBoxVisible.value = true;
    };

    // 关闭消息框
    const closeMessageBox = () => {
      messageBoxVisible.value = false;
    };

    // 触发文件选择框
    const triggerFileInput = () => {
      fileInput.value?.click();
    };

    const handleFileChange = (event) => {
      const file = event.target.files?.[0];
      if (!file) return;
      if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type)) {
        event.target.value = '';
        showMessageBox('头像仅支持 JPG、PNG 或 WebP 格式');
        return;
      }
      if (file.size > 5 * 1024 * 1024) {
        event.target.value = '';
        showMessageBox('头像大小不能超过 5MB');
        return;
      }

      uploadedFile.value = file;
      const reader = new FileReader();
      reader.onload = (e) => {
        avatar.value = e.target.result;
      };
      reader.onerror = () => {
        uploadedFile.value = null;
        avatar.value = DEFAULT_USER_AVATAR;
        toast.error('头像预览失败，请重新选择');
      };
      reader.readAsDataURL(file);
    };


    // 注册函数，包含所有校验和注册请求
    const register = async () => {
      // 客户端校验
      user.username = user.username.trim();
      if (!user.username) {
        showMessageBox('用户名不能为空！');
        return;
      }
      // 根据API文档，用户名长度应在1到20个字符之间
      if (user.username.length > 20) {
        showMessageBox('用户名过长，请勿超过20个字符！');
        return;
      }

      if (!user.familyName.trim()) {
        showMessageBox('姓不能为空！');
        return;
      }
      if (!user.givenName.trim()) {
        showMessageBox('名不能为空！');
        return;
      }

      if (!/^1[3-9]\d{9}$/.test(user.phone)) {
        showMessageBox('请输入11位有效手机号！');
        return;
      }

      const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
      if (!user.password || !passwordRegex.test(user.password)) {
        showMessageBox('密码格式错误，请确保包含至少一个大写字母、一个小写字母和一个数字，长度至少为8个字符。');
        return;
      }
      if (user.password !== confirmPassword.value) {
        showMessageBox('两次输入的密码不一致！');
        return;
      }
      
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      if (!user.useremail || !emailRegex.test(user.useremail)) {
        showMessageBox('邮箱格式不正确！');
        return;
      }

      const registerPayload = {
        username: user.username,
        password: user.password,
        phone: user.phone,
        email: user.useremail,
        firstName: user.givenName.trim(),
        lastName: user.familyName.trim(),
        gender: user.gender
      };

      if (submitting.value) return;
      submitting.value = true;
      try {
        let requestBody = registerPayload;
        if (uploadedFile.value) {
          requestBody = new FormData();
          requestBody.append('user', new Blob([JSON.stringify(registerPayload)], { type: 'application/json' }));
          requestBody.append('avatar', uploadedFile.value);
        }
        const response = await request.post('/api/register', requestBody);

        if (response && response.success) {
          showMessageBox('注册成功！');
          setTimeout(() => {
            router.push({ path: '/login', query: { role: 'user' } });
          }, 900);
        } else {
          let errorMessage = '注册失败！服务器返回了无效数据。';
          if (response && response.message) {
            errorMessage = `注册失败！原因：${response.message}`;
          }
          showMessageBox(errorMessage);
        }
      } catch (error) {
        console.error('注册请求发生错误:', error.response || error.message);
        showMessageBox(error.response?.data?.message || '注册失败，请稍后重试');
      } finally {
        submitting.value = false;
      }
    };

    return {
      user,
      confirmPassword,
      showPassword,
      showConfirmPassword,
      avatar,
      uploadedFile,
      submitting,
      fileInput,
      triggerFileInput,
      handleFileChange,
      register,
      messageBoxVisible,
      messageBoxMessage,
      closeMessageBox
    };
  }
};
</script>

<style scoped>
/* -------------------- 基础样式重置 -------------------- */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: 'Arial', sans-serif;
  background-color: #f0f2f5;
}

.wrapper {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  background-color: #f0f2f5;
  padding-bottom: 20px;
}

/* -------------------- header部分 -------------------- */
header {
  width: 100%;
  height: 15vw;
  max-height: 80px;
  background-color: #0097FF;
  color: #fff;
  font-size: clamp(20px, 5vw, 24px);
  position: fixed;
  left: 0;
  top: 0;
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

/* -------------------- 表单部分 -------------------- */
.form-box {
  width: 90%;
  max-width: 400px;
  background: #fff;
  margin-top: clamp(96px, 14vh, 130px);
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  list-style: none;
}

.form-item {
  display: flex;
  align-items: center; /* 确保所有表单项内容垂直居中 */
  margin-bottom: 20px;
}

.form-item-title {
  font-size: 16px;
  font-weight: 600;
  color: #444;
  flex-basis: 90px;
  flex-shrink: 0;
  padding-right: 10px; /* 标题与内容间距 */
}

.form-item-content {
  flex: 1;
}

.form-item-content input[type='text'],
.form-item-content input[type='password'] {
  width: 100%;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 8px; /* 8px圆角 */
  font-size: 16px;
  color: #333;
  transition: border-color 0.3s;
}

.form-item-content input[type='text']:focus,
.form-item-content input[type='password']:focus {
  outline: none;
  border-color: #0097FF;
}

/* 添加一个额外的 li 来实现空格 */
.form-box .gender-item {
  margin-top: 20px; /* 在性别选项上方增加外边距 */
  margin-bottom: 20px; /* 保持与下方按钮的距离 */
}

.form-item.gender-item .form-item-content {
  display: flex;
  align-items: center;
}

.form-item.gender-item .form-item-content input[type='radio'] {
  width: 16px;
  height: 16px;
  margin-right: 6px;
  vertical-align: middle;
  cursor: pointer;
}

.form-item.gender-item .form-item-content label {
  font-size: 14px;
  color: #666;
  margin-right: 15px;
  cursor: pointer;
}

/* -------------------- 新的头像上传样式 -------------------- */
.image-upload-area {
  position: relative;
  width: 80px;
  height: 80px;
  border: 2px dashed #ccc;
  border-radius: 50%; /* 圆形 */
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  transition: border-color 0.3s, background-color 0.3s;
  overflow: hidden;
}

.image-upload-area:hover {
  border-color: #0097ff;
  background-color: #f9f9f9;
}

.avatar-hint {
  margin-top: 7px;
  font-size: 12px;
  color: #8a99a8;
}

.image-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.change-avatar {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  padding: 3px 0;
  background: rgba(0, 0, 0, 0.52);
  color: #fff;
  font-size: 11px;
  line-height: 1.2;
  text-align: center;
}

.file-input {
  display: none; /* 隐藏原始文件输入框 */
}

/* -------------------- 字数提示样式 -------------------- */
.word-count {
  font-size: 12px;
  color: #999;
  text-align: right;
  margin-top: 4px;
}

/* -------------------- 注册按钮部分 -------------------- */
.button-register {
  width: 90%;
  max-width: 400px;
  margin-top: 20px;
}

.button-register button {
  width: 100%;
  height: 50px;
  font-size: 18px;
  font-weight: 700;
  color: #fff;
  background-color: #0097FF;
  border-radius: 8px;
  border: none;
  outline: none;
  cursor: pointer;
  transition: background-color 0.3s, transform 0.1s, box-shadow 0.3s;
  box-shadow: 0 4px 12px rgba(0, 151, 255, 0.3);
}

.button-register button:hover {
  background-color: #0097ff;
}

.button-register button:disabled {
  background: #83c8f7;
  box-shadow: none;
  cursor: not-allowed;
}

.button-register button:active {
  transform: translateY(1px);
}

/* -------------------- 自定义消息框 -------------------- */
.message-box-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 2000;
}

.message-box {
  background: #fff;
  padding: 24px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  width: 80%;
  max-width: 350px;
}

.message-box p {
  margin-bottom: 20px;
  font-size: 16px;
  color: #333;
}

.message-box button {
  background: #0097ff;
  color: #fff;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}
.back-btn-container {
  position: fixed; /* 固定定位，不随滚动移动 */
  left: 0vw; /* 距离左侧的距离，可根据需求调整 */
  top: 2vw; /* 距离顶部的距离，与 header 高度（12vw）适配，确保垂直居中 */
  z-index: 1001; /* 比 header 的 z-index:1000 高，避免被遮挡 */
}
.password-field{position:relative}.password-field input{padding-right:42px}.password-field button{position:absolute;right:4px;top:50%;transform:translateY(-50%);border:0;background:transparent;color:#718da4;padding:8px;cursor:pointer}
</style>
