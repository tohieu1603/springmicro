"use client";

import { useState } from "react";
import { Card, Form, Input, Button, Tabs } from "antd";
import { toast } from "sonner";

/**
 * Global SEO page — site-wide meta, robots, sitemap, redirects. Per-product
 * SEO lives on the product form's "SEO" tab.
 */
export default function SeoAdmin() {
  const [form] = Form.useForm();
  const [saving, setSaving] = useState(false);

  async function save() {
    setSaving(true);
    await new Promise((r) => setTimeout(r, 500));
    toast.success("Đã lưu SEO (mock)");
    setSaving(false);
  }

  return (
    <Card title="SEO toàn site" extra={<Button type="primary" onClick={save} loading={saving}>Lưu</Button>}>
      <Tabs
        items={[
          {
            key: "meta",
            label: "Meta mặc định",
            children: (
              <Form form={form} layout="vertical">
                <Form.Item name="siteTitle" label="Tiêu đề trang chủ">
                  <Input maxLength={70} showCount />
                </Form.Item>
                <Form.Item name="metaDescription" label="Mô tả mặc định">
                  <Input.TextArea maxLength={160} showCount rows={3} />
                </Form.Item>
                <Form.Item name="metaKeywords" label="Từ khoá">
                  <Input placeholder="ecommerce, vietnam, ..." />
                </Form.Item>
                <Form.Item name="ogImage" label="Open Graph image">
                  <Input placeholder="https://..." />
                </Form.Item>
              </Form>
            ),
          },
          {
            key: "robots",
            label: "Robots.txt",
            children: (
              <Form layout="vertical">
                <Form.Item label="Nội dung">
                  <Input.TextArea rows={10} defaultValue={`User-agent: *\nAllow: /\nDisallow: /admin/\nDisallow: /api/\nSitemap: /sitemap.xml`} />
                </Form.Item>
              </Form>
            ),
          },
          {
            key: "redirects",
            label: "Chuyển hướng",
            children: (
              <Form layout="vertical">
                <Form.List name="redirects">
                  {(fields, { add, remove }) => (
                    <>
                      {fields.map((f) => (
                        <div key={f.key} className="grid grid-cols-2 gap-3 mb-2">
                          <Form.Item name={[f.name, "from"]}><Input placeholder="/cu-tu" /></Form.Item>
                          <Form.Item name={[f.name, "to"]}><Input placeholder="/c/dinh-dang-moi" /></Form.Item>
                        </div>
                      ))}
                      <Button onClick={() => add({})}>+ Thêm chuyển hướng</Button>
                    </>
                  )}
                </Form.List>
              </Form>
            ),
          },
        ]}
      />
    </Card>
  );
}
