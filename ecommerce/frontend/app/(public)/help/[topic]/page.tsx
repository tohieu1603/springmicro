import type { Metadata } from "next";
import { notFound } from "next/navigation";
import Link from "next/link";
import { Container } from "@/components/ui/container";
import { Card, CardContent } from "@/components/ui/card";
import { Breadcrumb } from "@/components/ui/breadcrumb";
import { HELP_SECTIONS } from "../_help-data";
import { HelpAccordion } from "../accordion";

const KEYS = ["faq", "shipping", "returns", "payment"] as const;

export function generateStaticParams() {
  return KEYS.map((topic) => ({ topic }));
}

export async function generateMetadata({
  params,
}: { params: Promise<{ topic: string }> }): Promise<Metadata> {
  const { topic } = await params;
  const section = HELP_SECTIONS[topic];
  if (!section) return { title: "Trợ giúp — HIEU" };
  return {
    title: `${section.title} — HIEU`,
    description: section.intro,
    openGraph: {
      title: section.title,
      description: section.intro,
      type: "article",
      siteName: "HIEU",
    },
  };
}

export default async function HelpTopic({ params }: { params: Promise<{ topic: string }> }) {
  const { topic } = await params;
  const section = HELP_SECTIONS[topic];
  if (!section) notFound();

  return (
    <Container className="py-8 max-w-4xl">
      <Breadcrumb items={[{ href: "/help/faq", label: "Trợ giúp" }, { label: section.title }]} />

      <div className="grid lg:grid-cols-[200px,1fr] gap-8 mt-6">
        <aside className="space-y-1">
          {KEYS.map((k) => (
            <Link
              key={k}
              href={`/help/${k}`}
              className={
                "block px-3 py-2 rounded text-sm font-medium " +
                (k === topic ? "bg-primary text-white" : "hover:bg-surface-soft")
              }
            >
              {HELP_SECTIONS[k].title}
            </Link>
          ))}
        </aside>

        <div>
          <h1 className="text-h2-d">{section.title}</h1>
          <p className="text-slate mt-2">{section.intro}</p>
          <Card className="mt-6">
            <CardContent>
              <HelpAccordion items={section.faqs} />
            </CardContent>
          </Card>
        </div>
      </div>
    </Container>
  );
}
