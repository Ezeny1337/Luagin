import React, { useState, useEffect } from 'react';
import { Card, Tabs, Button, message, Space, Typography } from 'antd';
import { ReloadOutlined, FileTextOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import WebPanelConfig from './WebPanelConfig';
import MySQLConfig from './MySQLConfig';
import PerformanceConfig from './PerformanceConfig';
import PermissionsConfig from './PermissionsConfig';

const { TabPane } = Tabs;
const { Title } = Typography;

const cardStyle = {
  background: '#fff',
  borderRadius: 8,
  padding: 24,
  boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
  border: '1px solid #e5e6eb',
  minWidth: 0,
};

const Config: React.FC = () => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [configs, setConfigs] = useState<any>({});
  const [activeTab, setActiveTab] = useState('webpanel');

  // 获取所有配置
  const fetchConfigs = async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/config/all', { credentials: 'include' });

      if (response.ok) {
        const data = await response.json();
        setConfigs(data);
      } else {
        message.error(t('config.fetchFailed'));
      }
    } catch (error) {
      console.error('Failed to fetch configs:', error);
      message.error(t('config.fetchFailed'));
    } finally {
      setLoading(false);
    }
  };

  // 保存配置
  const saveConfig = async (configName: string, configData: any): Promise<any> => {
    setLoading(true);
    try {
      const response = await fetch(`/api/config/${configName}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify(configData),
      });

      if (response.ok) {
        const result = await response.json();

        message.success(t('config.saveSuccess'));

        await fetchConfigs(); // 重新获取配置
        return result;
      } else {
        message.error(t('config.saveFailed'));
        return { success: false };
      }
    } catch (error) {
      message.error(t('config.saveFailed'));
      return { success: false };
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchConfigs();
  }, []);

  const handleTabChange = (key: string) => {
    setActiveTab(key);
  };

  return (
    <div style={{ padding: '24px' }}>
      <Card style={cardStyle}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
          <Title level={3} style={{ margin: 0 }}>
            <FileTextOutlined style={{ marginRight: 8 }} />
            {t('config.title')}
          </Title>
          <Space>
            <Button 
              icon={<ReloadOutlined />} 
              onClick={fetchConfigs}
              loading={loading}
            >
              {t('config.refresh')}
            </Button>
          </Space>
        </div>

        <Tabs activeKey={activeTab} onChange={handleTabChange}>
          <TabPane tab={t('config.webpanel')} key="webpanel">
            <WebPanelConfig 
              config={configs.webpanel} 
              onSave={(data) => saveConfig('webpanel', data)}
              loading={loading}
            />
          </TabPane>
          <TabPane tab={t('config.mysql')} key="mysql">
            <MySQLConfig
              config={configs.mysql}
              onSave={(data) => saveConfig('mysql', data)}
              loading={loading}
            />
          </TabPane>
          <TabPane tab={t('config.performance')} key="performance">
            <PerformanceConfig
              config={configs.performance}
              onSave={(data) => saveConfig('performance', data)}
              loading={loading}
            />
          </TabPane>
          <TabPane tab={t('config.permissions')} key="permissions">
            <PermissionsConfig />
          </TabPane>
        </Tabs>
      </Card>
    </div>
  );
};

export default Config; 