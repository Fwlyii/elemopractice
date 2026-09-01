<template>
	<div class="wrapper">
		<header class="topbar"><p>商铺管理</p></header>
		<div class="content">
			<!-- 所有商家列表 -->
			<h2 class="section-title">商家列表</h2>
			<ul class="business-list">
				<li v-for="b in businessList" :key="b.businessId" class="business-item">
					<div class="info" @click="enterBusiness(b)">
						<img :src="b.businessImg || defaultImg" alt="logo" class="logo" @error="onImgError">
                        <div class="meta">
                            <p class="name" :class="{ disabled: b.disabled }">{{ b.businessName }}</p>
                            <p class="addr" :class="{ disabled: b.disabled }">{{ b.businessAddress || '地址未填写' }}</p>
                        </div>
					</div>
					<div class="actions">
						<button class="toggle" @click.stop="toggleEnable(b)">{{ b.disabled ? '启用' : '禁用' }}</button>
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
						<input v-model="editor.form.businessName" placeholder="请输入商铺名称" />
						<label>图片地址</label>
						<input v-model="editor.form.businessImg" placeholder="http(s)://..." />
						<label>商铺地址</label>
						<input v-model="editor.form.businessAddress" placeholder="请输入地址" />
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

export default {
	name: 'ManageBusiness',
	setup() {
    const businessList = ref([]);
		const storeList = ref([]);
		const currentBusiness = ref(null);
		const defaultImg = '/R-C.png';
    const router = useRouter();

		const editor = reactive({
			visible: false,
			mode: 'edit', // 'create' | 'edit'
			form: {
				businessId: null,
				businessName: '',
				businessImg: '',
				businessAddress: ''
			}
		});

		const onImgError = (e) => { e.target.src = defaultImg; };

		const loadBusinesses = async () => {
			try {
				const resp = await axios.get('BusinessController/listAll');
				businessList.value = Array.isArray(resp.data) ? resp.data : [];
				if (!businessList.value.length) throw new Error('empty');
			} catch (e) {
				// mock fallback
				businessList.value = [
					{ businessId: 1001, businessName: '美味小厨集团', businessImg: '', businessAddress: '天津市和平区', disabled: false },
					{ businessId: 1002, businessName: '街角奶茶集团', businessImg: '', businessAddress: '天津市南开区', disabled: true }
				];
			}
		};

    const enterBusiness = async (biz) => {
        router.push({ path: '/manageShop', query: { ownerId: biz.businessId, businessName: biz.businessName } });
    };

		const loadStores = async (ownerId) => {
			try {
				const resp = await axios.post('BusinessController/listStoresByOwner', { businessId: ownerId });
				storeList.value = Array.isArray(resp.data) ? resp.data : [];
				if (!storeList.value.length) throw new Error('empty');
			} catch (e) {
				// mock fallback
				storeList.value = [
					{ businessId: `${ownerId}-A`, businessName: '美味小厨（海光寺店）', businessImg: '', businessAddress: '和平区南京路188号' },
					{ businessId: `${ownerId}-B`, businessName: '美味小厨（天塔店）', businessImg: '', businessAddress: '河西区围堤道88号' }
				];
			}
		};

		const toggleEnable = async (biz) => {
			const next = !biz.disabled;
			try {
				await axios.post('BusinessController/toggleEnable', { businessId: biz.businessId, disabled: next });
				biz.disabled = next;
			} catch (e) {
				biz.disabled = next; // 前端演示
			}
		};

		const startEdit = (store) => {
			editor.mode = 'edit';
			editor.form = { businessId: store.businessId, businessName: store.businessName, businessImg: store.businessImg, businessAddress: store.businessAddress };
			editor.visible = true;
		};
		const startCreate = () => {
			editor.mode = 'create';
			editor.form = { businessId: null, businessName: '', businessImg: '', businessAddress: '' };
			editor.visible = true;
		};
		const closeEditor = () => { editor.visible = false; };

		const saveStore = async () => {
			try {
				if (editor.mode === 'create') {
					const resp = await axios.post('BusinessController/createStore', { ownerId: currentBusiness.value.businessId, ...editor.form });
					const saved = resp.data || { ...editor.form, businessId: Date.now() };
					storeList.value.push(saved);
				} else {
					await axios.post('BusinessController/updateStore', editor.form);
					const idx = storeList.value.findIndex(s => s.businessId === editor.form.businessId);
					if (idx >= 0) storeList.value[idx] = { ...storeList.value[idx], ...editor.form };
				}
				editor.visible = false;
			} catch (e) {
				// 前端演示保存
				if (editor.mode === 'create') {
					storeList.value.push({ ...editor.form, businessId: Date.now() });
				} else {
					const idx = storeList.value.findIndex(s => s.businessId === editor.form.businessId);
					if (idx >= 0) storeList.value[idx] = { ...storeList.value[idx], ...editor.form };
				}
				editor.visible = false;
			}
		};

		const removeStore = async (store) => {
			if (!confirm('确认删除该商铺吗？')) return;
			try {
				await axios.post('BusinessController/deleteStore', { businessId: store.businessId });
				storeList.value = storeList.value.filter(s => s.businessId !== store.businessId);
			} catch (e) {
				storeList.value = storeList.value.filter(s => s.businessId !== store.businessId);
			}
		};

		onMounted(() => {
			loadBusinesses();
		});

		return {
			businessList,
			storeList,
			currentBusiness,
			defaultImg,
			onImgError,
			enterBusiness,
			toggleEnable,
			editor
		};
	}
};
</script>

<style scoped>
.wrapper { width: 100%; min-height: 100vh; background: #fff; }
.topbar { width: 100%; height: 12vw; background: #409eff; color: #fff; font-size: 4.8vw; position: fixed; left: 0; top: 0; z-index: 1000; display: flex; justify-content: center; align-items: center; }
.content { margin-top: 12vw; padding: 4vw; }
.section-title { font-size: 4.2vw; margin: 2vw 0; }

.business-list, .store-list { list-style: none; padding: 0; margin: 0; }
.business-item, .store-item { display: flex; align-items: center; justify-content: space-between; padding: 3vw; border-bottom: 1px solid #f0f0f0; }
.logo { width: 14vw; height: 14vw; object-fit: cover; border-radius: 1vw; margin-right: 2vw; }
.info { display: flex; align-items: center; cursor: pointer; }
.meta { display: flex; flex-direction: column; }
.name { font-size: 4vw; color: #333; }
.addr { font-size: 3.2vw; color: #777; margin-top: .6vw; }
.name.disabled, .addr.disabled { color: #bbb; }
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
</style>


