<template>
  <div class="back-button" aria-label="返回" @click="goBack">
    <span class="back-arrow" aria-hidden="true">‹</span>
  </div>
</template>

<script>
import { useRouter } from 'vue-router';

export default {
  name: 'BackButton',
  setup() {
    const router = useRouter();
    const goBack = () => router.back();
    return { goBack };
  }
};
</script>

<style scoped>
.back-button {
  position: fixed;
  left: 16px;
  top: 14px;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: #fff;
  color: #167bc0;
  border: 1px solid #d7e8f6;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(31, 93, 145, .08);
  user-select: none;
  z-index: 1005;
  font-size: 16px;
}
.back-button:hover { background: #f1f8ff; color: #0097ff; }
.back-arrow { font-family: Arial, sans-serif; font-size: 30px; line-height: 1; transform: translateY(-1px); }
</style>
