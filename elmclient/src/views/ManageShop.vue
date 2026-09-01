<template>
	<div class="wrapper">
		<header class="topbar"><p>商铺管理 - {{ businessName || '商家' }}</p></header>
		<div class="content">
			<div class="toolbar">
				<button class="back" @click="goBack">返回</button>
			</div>
			<ul class="store-list">
				<li v-for="s in storeList" :key="s.businessId" class="store-item">
					<img :src="s.businessImg || defaultImg" class="logo" @error="onImgError" />
					<div class="meta">
						<p class="name">{{ s.businessName }}</p>
						<p class="addr">{{ s.businessAddress }}</p>
					</div>
					<div class="actions">
						<button class="edit" @click="startEdit(s)">编辑</button>
						<button class="del" @click="removeStore(s)">删除</button>
					</div>
				</li>
			</ul>

			<!-- 底部新增按钮 -->
			<div class="bottom-bar">
				<button class="add" @click="startCreate">新增商铺</button>
			</div>

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
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';

export default {
	name: 'ManageShop',
	setup() {
		const route = useRoute();
		const router = useRouter();
		const ownerId = ref(null);
		const businessName = ref('');
		const storeList = ref([]);
		const defaultImg = '/R-C.png';

		const editor = reactive({
			visible: false,
			mode: 'edit',
			form: { businessId: null, businessName: '', businessImg: '', businessAddress: '' }
		});

		const goBack = () => router.back();
		const onImgError = (e) => { e.target.src = defaultImg; };

		const loadStores = async () => {
			try {
				const resp = await axios.post('BusinessController/listStoresByOwner', { businessId: ownerId.value });
				storeList.value = Array.isArray(resp.data) ? resp.data : [];
				if (!storeList.value.length) throw new Error('empty');
			} catch (e) {
				storeList.value = [
					{ businessId: `${ownerId.value}-A`, businessName: `${businessName.value}（一店）`, businessImg: '', businessAddress: '和平区南京路188号' },
					{ businessId: `${ownerId.value}-B`, businessName: `${businessName.value}（二店）`, businessImg: '', businessAddress: '河西区围堤道88号' }
				];
			}
		};

		const startEdit = (store) => { editor.mode = 'edit'; editor.form = { ...store }; editor.visible = true; };
		const startCreate = () => { editor.mode = 'create'; editor.form = { businessId: null, businessName: '', businessImg: '', businessAddress: '' }; editor.visible = true; };
		const closeEditor = () => { editor.visible = false; };

		const saveStore = async () => {
			try {
				if (editor.mode === 'create') {
					const resp = await axios.post('BusinessController/createStore', { ownerId: ownerId.value, ...editor.form });
					const saved = resp.data || { ...editor.form, businessId: Date.now() };
					storeList.value.push(saved);
				} else {
					await axios.post('BusinessController/updateStore', editor.form);
					const idx = storeList.value.findIndex(s => s.businessId === editor.form.businessId);
					if (idx >= 0) storeList.value[idx] = { ...storeList.value[idx], ...editor.form };
				}
				editor.visible = false;
			} catch (e) {
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
			ownerId.value = route.query.ownerId ? String(route.query.ownerId) : null;
			businessName.value = route.query.businessName || '';
			loadStores();
		});

		return { businessName, storeList, defaultImg, onImgError, goBack, startEdit, startCreate, closeEditor, saveStore, removeStore, editor };
	}
};
</script>

<style scoped>
.wrapper { width: 100%; min-height: 100vh; background: #fff; }
.topbar { width: 100%; height: 12vw; background: #409eff; color: #fff; font-size: 4.8vw; position: fixed; left: 0; top: 0; z-index: 1000; display: flex; justify-content: center; align-items: center; }
.content { margin-top: 12vw; padding: 4vw; }
.toolbar { display: flex; gap: 2vw; margin-bottom: 2vw; }
.back { background: #eee; color: #333; border: none; border-radius: 1.2vw; padding: 1.6vw 3vw; font-size: 3.6vw; }
.add { background: #1e80ff; color: #fff; border: none; border-radius: 1.2vw; padding: 1.6vw 3vw; font-size: 3.6vw; }
.store-list { list-style: none; padding: 0; margin: 0; }
.store-item { display: flex; align-items: center; justify-content: space-between; padding: 3vw; border-bottom: 1px solid #f0f0f0; }
.logo { width: 14vw; height: 14vw; object-fit: cover; border-radius: 1vw; margin-right: 2vw; }
.meta { display: flex; flex-direction: column; }
.name { font-size: 4vw; color: #333; }
.addr { font-size: 3.2vw; color: #777; margin-top: .6vw; }
.actions { display: flex; flex-direction: row; gap: 2vw; align-items: center; }
.edit { background: #1e80ff; color: #fff; border: none; border-radius: 1.2vw; padding: 1.6vw 3vw; font-size: 3.6vw; }
.del { background: #fff; color: #e15656; border: 1px solid #f3caca; border-radius: 1.2vw; padding: 1.6vw 3vw; font-size: 3.6vw; }
.editor { position: fixed; inset: 0; background: rgba(0,0,0,.35); display: flex; align-items: center; justify-content: center; }
.editor .card { width: 86vw; background: #fff; border-radius: 1.6vw; padding: 4vw; }
.editor .form { display: flex; flex-direction: column; gap: 2vw; margin: 2vw 0; }
.editor .form label { font-size: 3.2vw; color: #555; }
.editor .form input { height: 9vw; font-size: 3.6vw; padding: 0 2vw; border: 1px solid #eee; border-radius: 1vw; }
.editor-actions { display: flex; justify-content: flex-end; gap: 2vw; }
.editor-actions .cancel { background: #eee; color: #333; border: none; border-radius: 1.2vw; padding: 1.6vw 3vw; font-size: 3.6vw; }
.editor-actions .save { background: #1e80ff; color: #fff; border: none; border-radius: 1.2vw; padding: 1.6vw 3vw; font-size: 3.6vw; }

.bottom-bar {
  position: fixed;
  right: 4vw;
  bottom: 18vw; /* 再往上，留更大底部间距 */
  background: transparent;
}
.bottom-bar .add {
  border-radius: 8vw;
  padding: 2.2vw 5vw;
  box-shadow: 0 6px 16px rgba(30,128,255,.35);
}
</style>


