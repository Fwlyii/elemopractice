<template>
	<div class="wrapper">
		<div class="top-background">
			<h1>商铺管理</h1>
		</div>
		<div class="content">
			<ul class="business-list">
				<li v-for="b in businessList" :key="b.userId" class="business-item">
					<div class="info" >
						<img :src="b.photo || defaultImg" alt="logo" class="logo" @error="onImgError">
                        <div class="meta">
                            <p class="name">{{ b.username }}集团</p>
                            <p class="addr">联系方式：{{ b.phone || '手机号未填写' }}</p>
                        </div>
					</div>
					<div class="actions">
						<button class="toggle" @click.stop="enterBusiness(b)">查看商铺</button>
					</div>
				</li>
			</ul>
		</div>
	</div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';	
export default {
    name: 'ManageBusiness',
    setup() {
        const businessList = ref([]);
        const defaultImg = require('@/assets/default-avatar.png');
        const router = useRouter();

        const onImgError = (e) => { 
            e.target.src = defaultImg; 
        };

        const loadBusinesses = async () => {
            try {
                const response = await request.get('/api/businesses/active');
                businessList.value = response;
            } catch (error) {
                console.error('获取商家列表失败:', error);
            }
        };

        const enterBusiness = (biz) => {
            router.push({ 
                path: '/admin/shop', 
                query: { 
                    ownerId: biz.userId, 
                    merchantName: biz.username + '集团' 
                } 
            });
        };

        onMounted(() => {
            loadBusinesses();
        });

        return {
            businessList,
            defaultImg,
            onImgError,
            enterBusiness
        };
    }
};
</script>

<style scoped>
.wrapper { 
  width: 100%; 
  min-height: 100vh; 
  background: #fff; 
  font-family: 'Helvetica Neue', Arial, sans-serif;
}

.top-background {
  width: 100%;
  height: 100px;
  background: linear-gradient(to right, #3a7bd5, #00d2ff);
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  border-radius: 16px 16px 0 0;
  position: fixed;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  overflow: hidden;
  margin-bottom: 50px;
  max-width: 600px;
}

.top-background::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0) 70%);
  transform: rotate(30deg);
  animation: shine 6s infinite linear;
}

@keyframes shine {
  0% {
    transform: rotate(30deg) translate(-10%, -10%);
  }
  100% {
    transform: rotate(30deg) translate(10%, 10%);
  }
}

.top-background h1 {
  color: white;
  font-size: 1.8rem;
  font-weight: 600;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  letter-spacing: 1px;
  margin: 0;
  z-index: 1;
}

.content { 
  margin-top: 10px; 
  padding: 4vw; 
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
  padding-bottom: 70px;
}
.business-list { list-style: none; padding: 0; margin: 0; }
.business-item { display: flex; align-items: center; justify-content: space-between; padding: 3vw; border-bottom: 1px solid #f0f0f0; }
.logo { width: 14vw; height: 14vw; object-fit: cover; border-radius: 1vw; margin-right: 2vw; }
.info { display: flex; align-items: center; cursor: pointer; }
.meta { display: flex; flex-direction: column; }
.name { font-size: 4vw; color: #333; }
.addr { font-size: 3.2vw; color: #777; margin-top: .6vw; }
.business-item .actions { display: flex; flex-direction: row; align-items: center; gap: 2vw; white-space: nowrap; }
.actions button { margin-left: 0; }
.toggle { background: #fff; color: #e15656; border: 1px solid #f3caca; border-radius: 1.2vw; padding: 1.6vw 3vw; font-size: 3.6vw; }

@media (max-width: 480px) {
  .wrapper {
    max-width: 100vw;
    width: 100vw;
  }
  
  .top-background {
    height: 90px;
    border-radius: 0;
    max-width: 100vw;
  }
  
  .content {
    margin-top: 90px;
    max-width: 100vw;
    width: 100vw;
  }
}

.wrapper { background: #f5f9fd; color: #24405c; }
.top-background { height: 64px; background: #0097ff; background-image: none; border-radius: 0; box-shadow: 0 1px 0 rgba(0,83,145,.15); }
.top-background::before { display: none; }
.top-background h1 { font-size: 20px; letter-spacing: 0; text-shadow: none; }
.content { margin-top: 0; padding: 84px 16px 72px; max-width: 600px; }
.content { box-sizing: border-box; overflow-x: hidden; }
.business-item { box-sizing: border-box; width: 100%; padding: 14px 0; border-bottom-color: #e1edf7; gap: 10px; }
.info { min-width: 0; flex: 1 1 auto; overflow: hidden; }
.meta { min-width: 0; }
.name { color: #24405c; font-size: 15px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.addr { color: #71879a; font-size: 12px; overflow-wrap: anywhere; }
.logo { width: 52px; height: 52px; margin-right: 0; border-radius: 8px; }
.business-item .actions { flex: 0 0 78px; }
.toggle { color: #0879c7; border-color: #a9d6f4; background: #f4fbff; padding: 7px 10px; font-size: 12px; }
@media (max-width: 480px) {
  .top-background { height: 64px; }
  .content { width: 100%; max-width: 100%; padding: 84px 12px 72px; }
  .toggle { width: 78px; padding-left: 6px; padding-right: 6px; }
}
</style>
