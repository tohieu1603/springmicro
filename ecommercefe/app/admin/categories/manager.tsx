"use client";

import { useMemo, useState } from "react";
import { Tree, Button, Modal, Form, Input, Space, TreeSelect, Popconfirm } from "antd";
import { PlusOutlined, EditOutlined, DeleteOutlined } from "@ant-design/icons";
import { toast } from "sonner";
import { api } from "@/lib/api/client";
import { buildCategoryTree, flattenTree } from "@/lib/categories";
import { slugify } from "@/lib/utils";
import type { Category } from "@/lib/api/types";

/**
 * Tree-based category editor.
 *
 * Local optimistic model: we keep the full flat list in state, the Tree
 * widget renders the derived tree, and create/update/delete each refetch
 * after success so any server-side sortOrder normalisation flows back.
 */
export function CategoriesManager({ initialTree }: { initialTree: Category[] }) {
  const [flat, setFlat] = useState<Category[]>(flattenTree(initialTree));
  const tree = useMemo(() => buildCategoryTree(flat), [flat]);
  const [editing, setEditing] = useState<Category | "new" | null>(null);
  const [parentForNew, setParentForNew] = useState<string | null>(null);

  async function refresh() {
    try {
      const res = await api.get<Category[] | { data: Category[] }>("/api/categories");
      const body = res.data as unknown as { data?: Category[] };
      setFlat(body?.data ?? (res.data as Category[]));
    } catch {
      toast.error("Không tải được danh mục");
    }
  }

  async function onDelete(id: string) {
    try {
      await api.delete(`/api/categories/${id}`);
      toast.success("Đã xóa");
      refresh();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Không xóa được — có thể đang dùng trong sản phẩm");
    }
  }

  const treeData = mapToAntTree(tree, {
    onEdit: (c) => setEditing(c),
    onDelete,
    onAddChild: (id) => {
      setParentForNew(id);
      setEditing("new");
    },
  });

  return (
    <>
      <Space className="mb-4">
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => {
            setParentForNew(null);
            setEditing("new");
          }}
        >
          Thêm danh mục gốc
        </Button>
      </Space>

      <Tree
        treeData={treeData}
        defaultExpandAll
        showLine
        blockNode
      />

      <CategoryDialog
        open={editing !== null}
        mode={editing === "new" ? "create" : "edit"}
        category={editing && editing !== "new" ? editing : undefined}
        parentDefault={parentForNew ?? (editing && editing !== "new" ? editing.parentId ?? null : null)}
        flat={flat}
        onClose={() => setEditing(null)}
        onSaved={() => {
          setEditing(null);
          refresh();
        }}
      />
    </>
  );
}

function mapToAntTree(
  nodes: Category[],
  actions: { onEdit: (c: Category) => void; onDelete: (id: string) => void; onAddChild: (id: string) => void },
): any[] {
  return nodes.map((n) => ({
    key: n.id,
    title: (
      <div className="inline-flex items-center gap-3 group">
        <span>{n.name}</span>
        <span className="text-xs text-slate">/{n.slug}</span>
        <Space size={4} className="opacity-0 group-hover:opacity-100 transition-opacity">
          <Button size="small" icon={<PlusOutlined />} onClick={() => actions.onAddChild(n.id)} />
          <Button size="small" icon={<EditOutlined />} onClick={() => actions.onEdit(n)} />
          <Popconfirm
            title={`Xóa "${n.name}"?`}
            onConfirm={() => actions.onDelete(n.id)}
            okText="Xóa"
            cancelText="Hủy"
            okButtonProps={{ danger: true }}
          >
            <Button size="small" icon={<DeleteOutlined />} danger />
          </Popconfirm>
        </Space>
      </div>
    ),
    children: n.children?.length ? mapToAntTree(n.children, actions) : undefined,
  }));
}

function CategoryDialog({
  open,
  mode,
  category,
  parentDefault,
  flat,
  onClose,
  onSaved,
}: {
  open: boolean;
  mode: "create" | "edit";
  category?: Category;
  parentDefault: string | null;
  flat: Category[];
  onClose: () => void;
  onSaved: () => void;
}) {
  const [form] = Form.useForm();

  // Reset form whenever the modal opens with a new payload.
  if (open) {
    setTimeout(() => {
      form.setFieldsValue({
        name: category?.name,
        slug: category?.slug,
        description: category?.description,
        parentId: category?.parentId ?? parentDefault ?? null,
        sortOrder: category?.sortOrder ?? 0,
      });
    }, 0);
  }

  async function submit() {
    const v = await form.validateFields();
    const payload = { ...v, slug: v.slug || slugify(v.name) };
    try {
      if (mode === "create") {
        await api.post("/api/categories", payload);
        toast.success("Đã tạo danh mục");
      } else if (category) {
        await api.patch(`/api/categories/${category.id}`, payload);
        toast.success("Đã cập nhật");
      }
      onSaved();
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string } } };
      toast.error(err.response?.data?.message || "Lưu thất bại");
    }
  }

  return (
    <Modal
      open={open}
      title={mode === "create" ? "Thêm danh mục" : "Chỉnh danh mục"}
      onCancel={onClose}
      onOk={submit}
      okText="Lưu"
      cancelText="Hủy"
      destroyOnClose
    >
      <Form form={form} layout="vertical">
        <Form.Item name="name" label="Tên danh mục" rules={[{ required: true }]}>
          <Input />
        </Form.Item>
        <Form.Item name="slug" label="Slug" tooltip="Để trống để tự sinh">
          <Input />
        </Form.Item>
        <Form.Item name="parentId" label="Danh mục cha">
          <TreeSelect
            allowClear
            placeholder="Không có (gốc)"
            treeData={mapTreeSelect(buildCategoryTree(flat).filter((c) => c.id !== category?.id))}
            treeDefaultExpandAll
          />
        </Form.Item>
        <Form.Item name="sortOrder" label="Thứ tự sắp xếp">
          <Input type="number" />
        </Form.Item>
        <Form.Item name="description" label="Mô tả">
          <Input.TextArea rows={3} />
        </Form.Item>
      </Form>
    </Modal>
  );
}

function mapTreeSelect(nodes: Category[]): any[] {
  return nodes.map((n) => ({
    value: n.id,
    title: n.name,
    children: n.children?.length ? mapTreeSelect(n.children) : undefined,
  }));
}
