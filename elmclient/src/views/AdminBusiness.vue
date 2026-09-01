<template>
	<div class="wrapper">
		<div class="top-background">
			<h1>商铺管理</h1>
		</div>
		<div class="content">
			<!-- 所有商家列表 -->
			<!-- <h2 class="section-title">商家列表</h2> -->
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

			<!-- 当前商家下的门店管理 -->
			<!-- 精简：仅展示商家，不在本页显示门店明细 -->

			<!-- 编辑/新增弹出层 -->
			<div v-if="editor.visible" class="editor">
				<div class="card">
					<h3>{{ editor.mode === 'create' ? '新增商铺' : '编辑商铺' }}</h3>
					<div class="form">
						<label>商铺名称</label>
						<input v-model="editor.form.username" placeholder="请输入商铺名称" />
						<label>图片地址</label>
						<input v-model="editor.form.photo" placeholder="http(s)://..." />
						<label>手机号</label>
						<input v-model="editor.form.phone" placeholder="请输入手机号" />
					</div>
					<div class="editor-actions">
						<button class="cancel" @click="closeEditor">取消</button>
						<button class="save" @click="saveStore">保存</button>
					</div>
				</div>
			</div>
		</div>
	</div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import request from '../utils/request';	
export default {
    name: 'ManageBusiness',
    setup() {
        const businessList = ref([]);
        const defaultImg = require('@/assets/default-avatar.png');
        const router = useRouter();

        const editor = reactive({
            visible: false,
            mode: 'edit', // 'create' | 'edit'
            form: {
                userId: null,
                username: '',
                photo: '',
                phone: ''
            }
        });

        // 图片加载失败处理
        const onImgError = (e) => { 
            e.target.src = defaultImg; 
        };

        // 加载商家列表（从后端接口获取）
        const loadBusinesses = async () => {
            try {
                const response = await request.get('http://110.42.60.144:8080/api/businesses/active');
                businessList.value = response;
				console.log('response.data:', response.data);
				console.log('response:', response);
                console.log('加载商家列表:', businessList.value);
            } catch (error) {
                console.error('获取商家列表失败:', error);
                // 可以在这里添加错误提示
            }
        };

        // 进入商家管理
        const enterBusiness = (biz) => {
            console.log('进入商家管理:', biz.username);
            router.push({ 
                path: '/admin/shop', 
                query: { 
                    ownerId: biz.userId, 
                    merchantName: biz.username + '集团' 
                } 
            });
        };

        // 编辑器相关方法
        const startEdit = (business) => {
            editor.mode = 'edit';
            editor.form = { 
                ...business 
            };
            editor.visible = true;
        };

        const startCreate = () => {
            editor.mode = 'create';
            editor.form = { 
                userId: null, 
                username: '', 
                photo: '', 
                phone: '' 
            };
            editor.visible = true;
        };

        const closeEditor = () => { 
            editor.visible = false; 
        };

        // 保存商家信息（需要对接后端接口）
        const saveStore = async () => {
            try {
                if (editor.mode === 'create') {
                    // 调用新增接口
                    const response = await request.post('http://110.42.60.144:8080/api/businesses', editor.form);
                    businessList.value.push(response.data);
                    console.log('新增商家:', response.data);
                } else {
                    // 调用更新接口
                    const response = await request.patch(`http://110.42.60.144:8080/api/businesses/${editor.form.userId}`, editor.form);
                    const index = businessList.value.findIndex(
                        b => b.userId === editor.form.userId
                    );
                    if (index >= 0) {
                        businessList.value[index] = response.data;
                        console.log('更新商家:', businessList.value[index]);
                    }
                }
                editor.visible = false;
            } catch (error) {
                console.error('保存商家信息失败:', error);
                // 可以在这里添加错误提示
            }
        };

        // 初始化加载数据
        onMounted(() => {
            loadBusinesses();
        });

        return {
            businessList,
            defaultImg,
            editor,
            onImgError,
            enterBusiness,
            startEdit,
            startCreate,
            closeEditor,
            saveStore
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
.section-title { font-size: 4.2vw; margin: 2vw 0; }

.business-list, .store-list { list-style: none; padding: 0; margin: 0; }
.business-item, .store-item { display: flex; align-items: center; justify-content: space-between; padding: 3vw; border-bottom: 1px solid #f0f0f0; }
.logo { width: 14vw; height: 14vw; object-fit: cover; border-radius: 1vw; margin-right: 2vw; }
.info { display: flex; align-items: center; cursor: pointer; }
.meta { display: flex; flex-direction: column; }
.name { font-size: 4vw; color: #333; }
.addr { font-size: 3.2vw; color: #777; margin-top: .6vw; }
.store-item .actions { display: flex; flex-direction: row; align-items: center; gap: 2vw; white-space: nowrap; }
.business-item .actions { display: flex; flex-direction: row; align-items: center; gap: 2vw; white-space: nowrap; }
.actions button { margin-left: 0; }
.toggle { background: #fff; color: #e15656; border: 1px solid #f3caca; border-radius: 1.2vw; padding: 1.6vw 3vw; font-size: 3.6vw; }
.edit { background: #1e80ff; color: #fff; border: none; border-radius: 1.2vw; padding: 1.6vw 3vw; font-size: 3.6vw; }
.del { background: #fff; color: #e15656; border: 1px solid #f3caca; border-radius: 1.2vw; padding: 1.6vw 3vw; font-size: 3.6vw; }

.toolbar { margin: 2vw 0; }
.add { background: #1e80ff; color: #fff; border: none; border-radius: 1.2vw; padding: 1.6vw 3vw; font-size: 3.6vw; }

.editor { position: fixed; inset: 0; background: rgba(0,0,0,.35); display: flex; align-items: center; justify-content: center; }
.editor .card { width: 86vw; background: #fff; border-radius: 1.6vw; padding: 4vw; }
.editor .form { display: flex; flex-direction: column; gap: 2vw; margin: 2vw 0; }
.editor .form label { font-size: 3.2vw; color: #555; }
.editor .form input { height: 9vw; font-size: 3.6vw; padding: 0 2vw; border: 1px solid #eee; border-radius: 1vw; }
.editor-actions { display: flex; justify-content: flex-end; gap: 2vw; }
.editor-actions .cancel { background: #eee; color: #333; border: none; border-radius: 1.2vw; padding: 1.6vw 3vw; font-size: 3.6vw; }
.editor-actions .save { background: #1e80ff; color: #fff; border: none; border-radius: 1.2vw; padding: 1.6vw 3vw; font-size: 3.6vw; }

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
</style>